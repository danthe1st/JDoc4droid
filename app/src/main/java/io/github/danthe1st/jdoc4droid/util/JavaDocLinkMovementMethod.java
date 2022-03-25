package io.github.danthe1st.jdoc4droid.util;

import android.text.Layout;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.function.Predicate;

public class JavaDocLinkMovementMethod extends LinkMovementMethod {
    private final Predicate<String> onLinkClickedListener;

    public JavaDocLinkMovementMethod(Predicate<String> onLinkClickedListener) {
        this.onLinkClickedListener = onLinkClickedListener;
    }

    //https://stackoverflow.com/a/50342669/10871900
    //https://gitlab.com/Commit451/LabCoat/commit/0da57c371815902f4ba24fcd7bceaa1e7a8d7bb7#1869e1cd937878326e16d1ab7139f68380c48172
    @Override
    public boolean onTouchEvent(TextView widget, android.text.Spannable buffer, android.view.MotionEvent event) {
        int action = event.getAction();

        //http://stackoverflow.com/questions/1697084/handle-textview-link-click-in-my-android-app
        if (action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
            if (link.length != 0) {
                String url = link[0].getURL();
                boolean handled = onLinkClickedListener.test(url);
                if (handled) {
                    return true;
                }
                return super.onTouchEvent(widget, buffer, event);
            }
        }
        return super.onTouchEvent(widget, buffer, event);
    }
}
