package com.kairokk.client;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.EditText;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public final class ShulkDirectConnectFragment extends Fragment {
	private final Screen parent;
	private EditText address;
	private TextView error;
	public ShulkDirectConnectFragment(Screen parent) { this.parent = parent; }

	@Nullable
	@Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable DataSet state) {
		Context context = requireContext(); FrameLayout root = new FrameLayout(context);
		LinearLayout panel = new LinearLayout(context); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(ShulkServerUi.px(context, 28), ShulkServerUi.px(context, 22), ShulkServerUi.px(context, 28), ShulkServerUi.px(context, 24)); panel.setBackground(ShulkServerUi.shape(panel, 0xE80B111C, 0xFF2A3446, 10));
		TextView eyebrow = ShulkServerUi.text(context, "K A I R O K K   ·   MULTIPLAYER", 12, 0xFF3D8BFF); eyebrow.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); panel.addView(eyebrow, ShulkServerUi.full(context, 23, 5));
		TextView title = ShulkServerUi.text(context, "Direct Connect", 29, 0xFFF4F0F2); title.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); panel.addView(title, ShulkServerUi.full(context, 43, 7));
		TextView copy = ShulkServerUi.text(context, "Join any Minecraft server without saving it to your list.", 14, 0xFF8B838E); copy.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); panel.addView(copy, ShulkServerUi.full(context, 28, 16));
		TextView label = ShulkServerUi.text(context, "SERVER ADDRESS", 12, 0xFF8B838E); label.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); panel.addView(label, ShulkServerUi.full(context, 22, 5));
		address = ShulkServerUi.input(context, "", "play.example.net"); panel.addView(address, ShulkServerUi.full(context, 48, 8));
		error = ShulkServerUi.text(context, "", 13, 0xFF3D8BFF); error.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); panel.addView(error, ShulkServerUi.full(context, 24, 12));
		LinearLayout actions = new LinearLayout(context); Button cancel = ShulkServerUi.button(context, "Cancel", this::cancel); Button connect = ShulkServerUi.primaryButton(context, "Connect", this::connect); actions.addView(cancel, ShulkServerUi.weighted(context, 44, true)); actions.addView(connect, ShulkServerUi.weighted(context, 44, false)); panel.addView(actions, ShulkServerUi.full(context, 44, 0));
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ShulkServerUi.px(context, 580), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER); params.bottomMargin = ShulkServerUi.px(context, 20); root.addView(panel, params);
		ShulkTitleFragment.addFooter(root, context); KairokkScreenTransitions.fadeIn(root); return root;
	}

	private void connect() {
		String value = address.getText().toString().trim(); if (!ServerAddress.isValidAddress(value)) { error.setText("Enter a valid server address"); address.requestFocus(); return; }
		ServerData data = new ServerData("Direct Server", value, ServerData.Type.OTHER);
		Core.executeOnMainThread(() -> ConnectScreen.startConnecting(parent, Minecraft.getInstance(), ServerAddress.parseString(value), data, true, null));
	}
	private void cancel() { Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(parent)); }
}
