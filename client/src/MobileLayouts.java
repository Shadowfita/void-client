import com.voidclient.mobile.MobileConfig;
import java.util.WeakHashMap;

/** Non-destructive fallback for fixed root containers. Child layout still uses the authored extent. */
final class MobileLayouts {
    private static final class Extent {
        int width, height, appliedWidth, appliedHeight;
        boolean constrained;
    }
    private static final WeakHashMap<Class46, Extent> extents = new WeakHashMap<>();
    static void size(Class46 w, int parentWidth, int parentHeight) {
        if (MobileNativeHud.size(w) || MobileNativeFlow.size(w) || MobileItemGridLayout.size(w) || MobileNativeHud.stretch(w,parentWidth,parentHeight)) return;
        if (!MobileConfig.enabled() || w.anInt774 != 0 || w.anInt834 != -1
                || w.anInt765 != 0 || parentWidth < 1 || parentHeight < 1) return;
        MobileItemGridLayout.restoreContent(w);
        Extent e = extents.get(w);
        if (e == null) {
            e = new Extent(); e.width = w.anInt698; e.height = w.anInt791;
            e.appliedWidth = w.anInt698; e.appliedHeight = w.anInt791;
            extents.put(w, e);
        }
        // Scripts remain authoritative when they explicitly change content extents.
        if (w.anInt698 != e.appliedWidth) e.width = w.anInt698;
        if (w.anInt791 != e.appliedHeight) e.height = w.anInt791;
        int authoredWidth = Math.max(0, w.anInt709), authoredHeight = Math.max(0, w.anInt789);
        e.constrained = authoredWidth > parentWidth || authoredHeight > parentHeight;
        w.anInt709 = Math.min(authoredWidth, parentWidth);
        w.anInt789 = Math.min(authoredHeight, parentHeight);
        w.anInt698 = authoredWidth > parentWidth ? Math.max(e.width, authoredWidth) : e.width;
        w.anInt791 = authoredHeight > parentHeight ? Math.max(e.height, authoredHeight) : e.height;
        e.appliedWidth = w.anInt698; e.appliedHeight = w.anInt791;
    }
    static void position(Class46 w, int width, int height) {
        if (MobileNativeHud.position(w) || MobileNativeFlow.position(w) || MobileItemGridLayout.position(w)) return;
        if (!MobileConfig.enabled()) return;
        Extent e = extents.get(w);
        if (e != null && e.constrained) {
            w.anInt800 = Math.max(0, Math.min(w.anInt800, width - w.anInt709));
            w.anInt750 = Math.max(0, Math.min(w.anInt750, height - w.anInt789));
        }
    }
    private MobileLayouts() { }
}
