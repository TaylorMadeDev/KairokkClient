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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import java.util.function.Consumer;

public final class ShulkServerFormFragment extends Fragment {
	private final Screen parent;
	private final String heading;
	private final ServerData server;
	private final Consumer<ServerData> onSave;
	private EditText name;
	private EditText address;
	private TextView error;
	private Button packMode;

	public ShulkServerFormFragment(Screen parent, String heading, ServerData server, Consumer<ServerData> onSave) {
		this.parent = parent; this.heading = heading; this.server = server; this.onSave = onSave;
	}

	@Nullable
	@Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable DataSet state) {
		Context context = requireContext(); FrameLayout root = new FrameLayout(context);
		LinearLayout panel = new LinearLayout(context); panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(ShulkServerUi.px(context, 28), ShulkServerUi.px(context, 22), ShulkServerUi.px(context, 28), ShulkServerUi.px(context, 24));
		panel.setBackground(ShulkServerUi.shape(panel, 0xE80B111C, 0xFF2A3446, 10));
		TextView eyebrow = label(context, "K A I R O K K   ·   MULTIPLAYER"); panel.addView(eyebrow, ShulkServerUi.full(context, 23, 5));
		TextView title = ShulkServerUi.text(context, heading, 29, 0xFFF4F0F2); title.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); panel.addView(title, ShulkServerUi.full(context, 43, 16));
		panel.addView(label(context, "SERVER NAME"), ShulkServerUi.full(context, 22, 5));
		name = ShulkServerUi.input(context, server.name, "My Server"); panel.addView(name, ShulkServerUi.full(context, 48, 13));
		panel.addView(label(context, "SERVER ADDRESS"), ShulkServerUi.full(context, 22, 5));
		address = ShulkServerUi.input(context, server.ip, "play.example.net"); panel.addView(address, ShulkServerUi.full(context, 48, 14));
		panel.addView(label(context, "SERVER RESOURCE PACKS"), ShulkServerUi.full(context, 22, 5));
		packMode = ShulkServerUi.button(context, packLabel(), this::cyclePackMode); panel.addView(packMode, ShulkServerUi.full(context, 44, 10));
		error = ShulkServerUi.text(context, "", 13, 0xFF3D8BFF); error.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); panel.addView(error, ShulkServerUi.full(context, 24, 10));
		LinearLayout actions = new LinearLayout(context); Button cancel = ShulkServerUi.button(context, "Cancel", this::cancel); Button save = ShulkServerUi.primaryButton(context, "Save Server", this::save);
		actions.addView(cancel, ShulkServerUi.weighted(context, 44, true)); actions.addView(save, ShulkServerUi.weighted(context, 44, false)); panel.addView(actions, ShulkServerUi.full(context, 44, 0));
		FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(ShulkServerUi.px(context, 580), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER); panelParams.bottomMargin = ShulkServerUi.px(context, 20); root.addView(panel, panelParams);
		ShulkTitleFragment.addFooter(root, context);
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	private TextView label(Context context, String value) { TextView label = ShulkServerUi.text(context, value, 12, 0xFF8B838E); label.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); return label; }
	private String packLabel() { return switch (server.getResourcePackStatus()) { case ENABLED -> "Enabled"; case DISABLED -> "Disabled"; case PROMPT -> "Prompt"; }; }
	private void cyclePackMode() {
		server.setResourcePackStatus(switch (server.getResourcePackStatus()) { case PROMPT -> ServerData.ServerPackStatus.ENABLED; case ENABLED -> ServerData.ServerPackStatus.DISABLED; case DISABLED -> ServerData.ServerPackStatus.PROMPT; });
		packMode.setText(packLabel());
	}
	private void save() {
		String serverName = name.getText().toString().trim(); String serverAddress = address.getText().toString().trim();
		if (serverName.isBlank()) { error.setText("Give this server a name"); name.requestFocus(); return; }
		if (!ServerAddress.isValidAddress(serverAddress)) { error.setText("Enter a valid server address"); address.requestFocus(); return; }
		server.name = serverName; server.ip = serverAddress; onSave.accept(server);
	}
	private void cancel() { Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(parent)); }
}
