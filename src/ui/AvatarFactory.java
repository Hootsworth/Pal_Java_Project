package ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import model.UserProfile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class AvatarFactory {

    private AvatarFactory() {
    }

    public static String buildDiceBearUrl(UserProfile profile) {
        if (profile == null) {
            return buildDiceBearUrl("bottts", "guest");
        }
        return buildDiceBearUrl(profile.getAvatarStyle(), profile.getAvatarSeed());
    }

    public static String buildDiceBearUrl(String style, String seed) {
        String safeStyle = (style == null || style.isBlank()) ? "bottts" : style;
        String safeSeed = (seed == null || seed.isBlank()) ? "guest" : seed;
        String encodedSeed = URLEncoder.encode(safeSeed, StandardCharsets.UTF_8);
        return "https://api.dicebear.com/9.x/" + safeStyle + "/png?seed=" + encodedSeed;
    }

    public static ImageView createCircularAvatar(UserProfile profile, double size) {
        ImageView avatar = new ImageView(new Image(buildDiceBearUrl(profile), true));
        avatar.setFitWidth(size);
        avatar.setFitHeight(size);
        avatar.setPreserveRatio(true);

        Circle clip = new Circle(size / 2, size / 2, size / 2);
        avatar.setClip(clip);
        return avatar;
    }
}
