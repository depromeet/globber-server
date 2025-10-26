package backend.globber.diary.domain.constant;

public enum PhotoTag {
    FOOD("FOOD"),
    SCENERY("SCENERY"),
    PEOPLE("PEOPLE"),
    NONE("NONE");

    final String tag;

    PhotoTag(String tag) {
        this.tag = tag;
    }
}
