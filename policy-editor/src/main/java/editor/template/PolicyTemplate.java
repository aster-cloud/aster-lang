package editor.template;

/**
 * 策略模板元数据
 */
public record PolicyTemplate(
    String name,
    String description,
    String category,
    String content
) {}
