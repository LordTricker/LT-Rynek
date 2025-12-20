package pl.lordtricker.ltrynek.core.scan;

public class ScanResult {
    public final boolean highlight;
    public final int color;

    public ScanResult(boolean highlight, int color) {
        this.highlight = highlight;
        this.color = color;
    }

    public static ScanResult noHighlight() {
        return new ScanResult(false, 0);
    }
}
