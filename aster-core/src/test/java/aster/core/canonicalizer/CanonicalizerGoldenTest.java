package aster.core.canonicalizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 黄金测试：对比 Java Canonicalizer 与 TypeScript 版本的输出
 * <p>
 * 在所有 cnl/examples/*.aster 文件上验证 Java 实现与 TypeScript 实现输出完全一致。
 * <p>
 * <b>验收标准</b>：100% 输出匹配
 */
class CanonicalizerGoldenTest {

    private Canonicalizer canonicalizer;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        canonicalizer = new Canonicalizer();
    }

    @Test
    void testAllExamplesMatchTypeScriptOutput() throws IOException {
        // 获取项目根目录
        Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
        Path examplesDir = projectRoot.resolve("cnl/examples");

        if (!Files.exists(examplesDir)) {
            fail("cnl/examples 目录不存在: " + examplesDir);
        }

        // 获取所有 .aster 文件
        List<Path> asterFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(examplesDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".aster"))
                 .forEach(asterFiles::add);
        }

        assertTrue(asterFiles.size() > 0, "应该至少有一个 .aster 测试文件");

        int totalFiles = asterFiles.size();
        int passedFiles = 0;
        List<String> failures = new ArrayList<>();

        // 对每个文件进行测试
        for (Path asterFile : asterFiles) {
            String relativePath = examplesDir.relativize(asterFile).toString();

            try {
                // 读取原始文件
                String input = Files.readString(asterFile);

                // Java Canonicalizer 输出
                String javaOutput = canonicalizer.canonicalize(input);

                // TypeScript Canonicalizer 输出（通过 Node.js 调用）
                String tsOutput = getTypeScriptOutput(asterFile, projectRoot);

                // 比较输出
                if (javaOutput.equals(tsOutput)) {
                    passedFiles++;
                    System.out.println("✓ " + relativePath);
                } else {
                    failures.add(String.format(
                        "%s: 输出不匹配\nJava 长度: %d\nTS 长度: %d\n首个不同字符位置: %d",
                        relativePath,
                        javaOutput.length(),
                        tsOutput.length(),
                        findFirstDifference(javaOutput, tsOutput)
                    ));
                    System.out.println("✗ " + relativePath);
                }
            } catch (Exception e) {
                failures.add(relativePath + ": " + e.getMessage());
                System.out.println("✗ " + relativePath + " (异常: " + e.getMessage() + ")");
            }
        }

        // 输出统计
        System.out.println("\n=== Golden Test Summary ===");
        System.out.println("Total files: " + totalFiles);
        System.out.println("Passed: " + passedFiles);
        System.out.println("Failed: " + (totalFiles - passedFiles));
        System.out.println("Pass rate: " + String.format("%.1f%%", 100.0 * passedFiles / totalFiles));

        // 如果有失败，输出详细信息
        if (!failures.isEmpty()) {
            System.out.println("\n=== Failures ===");
            for (String failure : failures) {
                System.out.println(failure);
            }
            fail(String.format("%d/%d 文件输出不匹配", failures.size(), totalFiles));
        }
    }

    /**
     * 通过 Node.js 调用 TypeScript Canonicalizer
     */
    private String getTypeScriptOutput(Path asterFile, Path projectRoot) throws IOException, InterruptedException {
        // 创建临时脚本
        Path scriptFile = tempDir.resolve("canonicalize-test.js");
        String script = String.format("""
            const fs = require('fs');
            const { canonicalize } = require('%s/dist/src/canonicalizer.js');

            const input = fs.readFileSync('%s', 'utf-8');
            const output = canonicalize(input);
            console.log(output);
            """,
            projectRoot.toString().replace("\\", "\\\\"),
            asterFile.toString().replace("\\", "\\\\")
        );
        Files.writeString(scriptFile, script);

        // 执行 Node.js
        ProcessBuilder pb = new ProcessBuilder("node", scriptFile.toString());
        pb.directory(projectRoot.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append('\n');
                }
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Node.js 执行失败 (exit code " + exitCode + "): " + output);
        }

        return output.toString();
    }

    /**
     * 找到两个字符串首次不同的位置
     */
    private int findFirstDifference(String s1, String s2) {
        int minLen = Math.min(s1.length(), s2.length());
        for (int i = 0; i < minLen; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }
        return minLen;
    }

    /**
     * 单个文件的快速测试（用于调试）
     */
    @Test
    void testSingleExample_Hello() throws IOException, InterruptedException {
        Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
        Path helloFile = projectRoot.resolve("cnl/examples/hello.aster");

        if (!Files.exists(helloFile)) {
            System.out.println("跳过测试: hello.aster 不存在");
            return;
        }

        String input = Files.readString(helloFile);
        String javaOutput = canonicalizer.canonicalize(input);
        String tsOutput = getTypeScriptOutput(helloFile, projectRoot);

        assertEquals(tsOutput, javaOutput,
            "hello.aster 的输出应该匹配 TypeScript 版本");
    }
}
