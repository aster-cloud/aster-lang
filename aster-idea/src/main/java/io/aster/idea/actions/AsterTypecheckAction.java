package io.aster.idea.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import io.aster.idea.icons.AsterIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 通过 npm 脚本执行类型检查，帮助开发者快速验证当前 .aster 文件。
 */
public final class AsterTypecheckAction extends AnAction {

  private static final Logger LOG = Logger.getInstance(AsterTypecheckAction.class);
  private static final String NOTIFICATION_GROUP_ID = "Aster CLI";

  public AsterTypecheckAction() {
    super("Aster: 类型检查当前文件", "使用 npm run typecheck:file 对当前文件执行类型检查", AsterIcons.FILE);
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
    boolean available = file != null && "aster".equalsIgnoreCase(file.getExtension());
    event.getPresentation().setEnabledAndVisible(available);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
    if (project == null || file == null) {
      return;
    }

    String basePath = project.getBasePath();
    if (basePath == null) {
      notify(project, NotificationType.ERROR, "无法定位项目根目录", "请在已打开的 Aster 项目中运行该操作。");
      return;
    }

    Path projectPath = Paths.get(basePath);
    Path ioFile = Paths.get(file.getPath());

    Path commandTarget;
    if (ioFile.startsWith(projectPath)) {
      commandTarget = projectPath.relativize(ioFile);
    } else {
      commandTarget = ioFile;
    }

    List<String> command = new ArrayList<>();
    command.add("npm");
    command.add("run");
    command.add("typecheck:file");
    command.add("--");
    command.add(commandTarget.toString());

    GeneralCommandLine commandLine = new GeneralCommandLine(command)
      .withCharset(StandardCharsets.UTF_8)
      .withWorkDirectory(projectPath.toFile());

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Aster 类型检查", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        indicator.setText("正在调用 npm run typecheck:file ...");

        CapturingProcessHandler handler;
        try {
          handler = new CapturingProcessHandler(commandLine);
        } catch (ExecutionException e) {
          LOG.warn("无法启动 npm 进程", e);
          notifyLater(project, NotificationType.ERROR, "无法启动 npm", Objects.toString(e.getMessage(), "未知错误"));
          return;
        }

        ProcessOutput output = handler.runProcessWithProgressIndicator(indicator);
        if (output.isCancelled()) {
          notifyLater(project, NotificationType.WARNING, "类型检查已取消", compress(output.getStdout()));
          return;
        }

        if (output.getExitCode() == 0) {
          String message = output.getStdout().isBlank()
            ? "类型检查通过"
            : compress(output.getStdout());
          notifyLater(project, NotificationType.INFORMATION, "类型检查通过", message);
        } else {
          StringBuilder builder = new StringBuilder();
          if (!output.getStdout().isBlank()) {
            builder.append(output.getStdout().trim());
          }
          if (!output.getStderr().isBlank()) {
            if (builder.length() > 0) {
              builder.append('\n');
            }
            builder.append(output.getStderr().trim());
          }
          String message = builder.length() == 0 ? "请查看 Run 控制台输出" : builder.toString();
          notifyLater(project, NotificationType.ERROR, "类型检查失败", compress(message));
        }
      }
    });
  }

  private static void notify(Project project, NotificationType type,
                             @NlsContexts.NotificationTitle String title,
                             @NlsContexts.NotificationContent String content) {
    NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID);
    group.createNotification(title, content, type).notify(project);
  }

  private static void notifyLater(Project project, NotificationType type,
                                  @NlsContexts.NotificationTitle String title,
                                  @NlsContexts.NotificationContent String content) {
    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(
      () -> notify(project, type, title, content)
    );
  }

  private static String compress(String text) {
    String trimmed = text.trim();
    if (trimmed.length() <= 800) {
      return trimmed;
    }
    return trimmed.substring(0, 800) + "\n...";
  }
}
