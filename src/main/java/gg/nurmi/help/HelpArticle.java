package gg.nurmi.help;

import java.util.List;

public record HelpArticle(String key, String title, List<String> body) {
}
