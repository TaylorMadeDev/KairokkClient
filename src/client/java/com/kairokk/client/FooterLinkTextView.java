package com.kairokk.client;

import icyllis.modernui.core.Context;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.PointerIcon;
import icyllis.modernui.widget.TextView;

/** Text-only footer action with the same hand cursor used by buttons. */
public final class FooterLinkTextView extends TextView {
	public FooterLinkTextView(Context context) { super(context); }

	@Override
	public PointerIcon onResolvePointerIcon(MotionEvent event) {
		return PointerIcon.getSystemIcon(PointerIcon.TYPE_HAND);
	}
}
