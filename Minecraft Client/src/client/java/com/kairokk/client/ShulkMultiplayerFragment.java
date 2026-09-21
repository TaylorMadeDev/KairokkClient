package com.kairokk.client;

import com.kairokk.KairokkClient;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.PopupWindow;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.network.EventLoopGroupHolder;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/** Shulk's saved-server browser backed by Minecraft's real servers.dat file. */
public final class ShulkMultiplayerFragment extends Fragment {
	private static final int CONTENT_WIDTH_DP = 1000;
	private final Screen parent;
	private final ServerStatusPinger pinger = new ServerStatusPinger();
	private final List<ServerRow> rows = new ArrayList<>();
	private ServerList servers;
	private LinearLayout serverRows;
	private TextView emptyState;
	private TextView status;
	private Button join;
	private Button edit;
	private Button delete;
	private ServerRow selected;
	private boolean alive;

	public ShulkMultiplayerFragment(Screen parent) { this.parent = parent; }

	@Nullable
	@Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable DataSet state) {
		alive = true;
		Context context = requireContext();
		FrameLayout root = new FrameLayout(context);
		LinearLayout content = new LinearLayout(context);
		content.setOrientation(LinearLayout.VERTICAL);
		content.setGravity(Gravity.CENTER_HORIZONTAL);

		content.addView(brand(context), ShulkServerUi.full(context, 74, 0));
		TextView heading = ShulkServerUi.text(context, "Play Multiplayer", 29, 0xFFF4F0F2);
		content.addView(heading, ShulkServerUi.full(context, 40, 3));
		status = ShulkServerUi.text(context, "Select a server, or add a new one", 14, 0xFF88808B);
		content.addView(status, ShulkServerUi.full(context, 24, 12));

		ScrollView scroll = new ScrollView(context);
		serverRows = new LinearLayout(context);
		serverRows.setOrientation(LinearLayout.VERTICAL);
		emptyState = ShulkServerUi.text(context, "Loading saved servers…", 17, 0xFF817984);
		serverRows.addView(emptyState, ShulkServerUi.full(context, 70, 0));
		scroll.addView(serverRows, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		scroll.setBackground(ShulkServerUi.shape(scroll, 0xA60D0B10, 0xFF29232C, 9));
		content.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

		content.addView(actions(context), ShulkServerUi.full(context, 102, 0));
		int requestedWidth = ShulkServerUi.px(context, CONTENT_WIDTH_DP);
		int responsiveWidth = Math.min(requestedWidth, Minecraft.getInstance().getWindow().getScreenWidth() - ShulkServerUi.px(context, 48));
		FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(Math.max(ShulkServerUi.px(context, 560), responsiveWidth),
			ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
		contentParams.topMargin = ShulkServerUi.px(context, 12);
		contentParams.bottomMargin = ShulkServerUi.px(context, ShulkServerUi.FOOTER_HEIGHT_DP + 8);
		root.addView(content, contentParams);
		ShulkTitleFragment.addFooter(root, context);
		loadServers();
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	@Override public void onDestroyView() { alive = false; stopPinger(); super.onDestroyView(); }

	private View brand(Context context) {
		LinearLayout brand = new LinearLayout(context); brand.setGravity(Gravity.CENTER);
		ShulkIconView mark = new ShulkIconView(context, ShulkIcon.BRAND, 0xFF3D8BFF, "Kairokk");
		brand.addView(mark, new LinearLayout.LayoutParams(ShulkServerUi.px(context, 56), ViewGroup.LayoutParams.MATCH_PARENT));
		TextView name = ShulkServerUi.text(context, "K A I R O K K", 31, 0xFFF4F0F2);
		brand.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
		TextView version = ShulkServerUi.text(context, "v0.1.0", 12, 0xFF3D8BFF);
		version.setPadding(ShulkServerUi.px(context, 7), 0, 0, 0);
		brand.addView(version, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
		return brand;
	}

	private View actions(Context context) {
		LinearLayout stack = new LinearLayout(context); stack.setOrientation(LinearLayout.VERTICAL); stack.setPadding(0, ShulkServerUi.px(context, 10), 0, 0);
		LinearLayout top = new LinearLayout(context);
		join = ShulkServerUi.primaryButton(context, "Join Server", this::joinSelected); join.setEnabled(false);
		Button direct = ShulkServerUi.button(context, "Direct Connect", this::directConnect);
		Button add = ShulkServerUi.button(context, "Add Server", this::addServer);
		top.addView(join, third(context)); top.addView(direct, third(context)); top.addView(add, third(context));
		stack.addView(top, ShulkServerUi.full(context, 42, 8));
		LinearLayout bottom = new LinearLayout(context);
		edit = ShulkServerUi.button(context, "Edit", this::editSelected); edit.setEnabled(false);
		delete = ShulkServerUi.button(context, "Delete", this::confirmDelete); delete.setEnabled(false);
		Button refresh = ShulkServerUi.button(context, "Refresh", this::loadServers);
		Button back = ShulkServerUi.button(context, "Back", this::goBack);
		for (Button button : new Button[]{edit, delete, refresh, back}) bottom.addView(button, quarter(context));
		stack.addView(bottom, ShulkServerUi.full(context, 42, 0));
		return stack;
	}

	private LinearLayout.LayoutParams third(Context context) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1); p.leftMargin = p.rightMargin = ShulkServerUi.px(context, 4); return p; }
	private LinearLayout.LayoutParams quarter(Context context) { return third(context); }

	private void loadServers() {
		stopPinger();
		servers = new ServerList(Minecraft.getInstance());
		servers.load();
		serverRows.removeAllViews(); rows.clear(); selected = null; updateActions();
		if (servers.size() == 0) {
			emptyState.setText("No saved servers yet — add one or connect directly");
			serverRows.addView(emptyState, ShulkServerUi.full(requireContext(), 76, 0));
			status.setText("Ready");
			return;
		}
		for (int i = 0; i < servers.size(); i++) {
			ServerRow row = new ServerRow(requireContext(), i, servers.get(i)); rows.add(row);
			LinearLayout.LayoutParams params = ShulkServerUi.full(requireContext(), 108, 8);
			params.leftMargin = params.rightMargin = ShulkServerUi.px(requireContext(), 7);
			if (i == 0) params.topMargin = ShulkServerUi.px(requireContext(), 7);
			serverRows.addView(row.view, params);
			ping(row);
		}
		status.setText(servers.size() + (servers.size() == 1 ? " saved server" : " saved servers"));
	}

	private void ping(ServerRow row) {
		row.detail.setText(row.data.ip + "  ·  Pinging…");
		try {
			pinger.pingServer(row.data, () -> updatePing(row), () -> updatePing(row), EventLoopGroupHolder.remote(false));
		} catch (UnknownHostException error) {
			row.detail.setText(row.data.ip + "  ·  Address not found");
		} catch (Exception error) {
			KairokkClient.LOGGER.debug("Could not ping {}", row.data.ip, error);
			row.detail.setText(row.data.ip + "  ·  Offline");
		}
	}

	private void updatePing(ServerRow row) {
		Core.executeOnUiThread(() -> {
			if (!alive) return;
			String result = row.data.ping >= 0 ? row.data.ping + " ms" : "Offline";
			String motd = cleanMotd(row.data.motd == null ? "" : row.data.motd.getString());
			if (motd.length() > 72) motd = motd.substring(0, 72) + "…";
			row.detail.setText(row.data.ip + "  ·  " + result);
			row.motd.setText(motd.isBlank() ? "No server description" : motd);
			row.icon.setServerIcon(row.data.getIconBytes());
		});
	}

	private static String cleanMotd(String value) {
		return value.replace('\n', ' ').replace('\r', ' ').replaceAll("(?i)§[0-9A-FK-ORX]", "").replaceAll("\\s+", " ").trim();
	}

	void tickPinger() { try { pinger.tick(); } catch (Exception ignored) { } }
	void stopPinger() { try { pinger.removeAll(); } catch (Exception ignored) { } }

	private void select(ServerRow row) {
		if (selected == row) { joinSelected(); return; }
		selected = row; for (ServerRow candidate : rows) candidate.view.setBackground(ShulkServerUi.rowBackground(candidate.view, candidate == row));
		status.setText(row.data.name + " selected"); updateActions();
	}

	private void updateActions() { boolean enabled = selected != null; if (join != null) join.setEnabled(enabled); if (edit != null) edit.setEnabled(enabled); if (delete != null) delete.setEnabled(enabled); }

	private void joinSelected() { if (selected != null) connect(selected.data); }
	private void connect(ServerData data) {
		if (!ServerAddress.isValidAddress(data.ip)) { status.setText("Enter a valid server address"); return; }
		Core.executeOnMainThread(() -> {
			Minecraft minecraft = Minecraft.getInstance(); Screen current = minecraft.screen;
			ConnectScreen.startConnecting(current, minecraft, ServerAddress.parseString(data.ip), data, false, null);
		});
	}

	private void directConnect() { Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(new ShulkDirectConnectScreen(Minecraft.getInstance().screen))); }
	private void addServer() {
		ServerData data = new ServerData("Minecraft Server", "", ServerData.Type.OTHER);
		openForm("Add Server", data, saved -> { servers.add(saved, false); servers.save(); returnToFreshList(); });
	}

	private void editSelected() {
		if (selected == null) return;
		int index = selected.index; ServerData copy = new ServerData(selected.data.name, selected.data.ip, ServerData.Type.OTHER); copy.copyFrom(selected.data);
		openForm("Edit Server", copy, saved -> { servers.replace(index, saved); servers.save(); returnToFreshList(); });
	}

	private void openForm(String title, ServerData data, java.util.function.Consumer<ServerData> save) {
		Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(new ShulkServerFormScreen(Minecraft.getInstance().screen, title, data, save)));
	}

	private void confirmDelete() {
		if (selected == null) return;
		Context context = requireContext(); ServerRow target = selected;
		LinearLayout card = new LinearLayout(context); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(ShulkServerUi.px(context, 18), ShulkServerUi.px(context, 14), ShulkServerUi.px(context, 18), ShulkServerUi.px(context, 16));
		card.setBackground(ShulkServerUi.shape(card, 0xFF171319, 0xFF493039, 9));
		TextView title = ShulkServerUi.text(context, "Delete " + target.data.name + "?", 19, 0xFFF4F0F2); title.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		TextView note = ShulkServerUi.text(context, "This removes it from your saved server list.", 13, 0xFF8B838E); note.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		card.addView(title, ShulkServerUi.full(context, 34, 2)); card.addView(note, ShulkServerUi.full(context, 28, 10));
		LinearLayout actions = new LinearLayout(context); PopupWindow popup = new PopupWindow(card, ShulkServerUi.px(context, 390), ShulkServerUi.px(context, 142), true);
		Button cancel = ShulkServerUi.button(context, "Cancel", popup::dismiss);
		Button remove = ShulkServerUi.primaryButton(context, "Delete", () -> { popup.dismiss(); servers.remove(target.data); servers.save(); loadServers(); });
		actions.addView(cancel, ShulkServerUi.weighted(context, 40, true)); actions.addView(remove, ShulkServerUi.weighted(context, 40, false)); card.addView(actions, ShulkServerUi.full(context, 40, 0));
		popup.setOutsideTouchable(true); popup.setBackgroundDrawable(new ShapeDrawable()); popup.showAtLocation(serverRows, Gravity.CENTER, 0, 0);
	}

	private void returnToFreshList() { Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(new ShulkMultiplayerScreen(parent))); }
	private void goBack() { Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(parent)); }

	private final class ServerRow {
		final int index; final ServerData data; final LinearLayout view; final ServerIconView icon; final TextView detail; final TextView motd;
		ServerRow(Context context, int index, ServerData data) {
			this.index = index; this.data = data; view = new LinearLayout(context); view.setOrientation(LinearLayout.HORIZONTAL); view.setGravity(Gravity.CENTER_VERTICAL);
			view.setPadding(ShulkServerUi.px(context, 13), ShulkServerUi.px(context, 5), ShulkServerUi.px(context, 13), ShulkServerUi.px(context, 5)); view.setClickable(true); view.setBackground(ShulkServerUi.rowBackground(view, false));
			KairokkUiSounds.attach(view);
			icon = new ServerIconView(context); icon.setBackground(ShulkServerUi.shape(icon, 0xFF17151C, 0xFF4B5870, 7));
			icon.setServerIcon(data.getIconBytes());
			view.addView(icon, new LinearLayout.LayoutParams(ShulkServerUi.px(context, 64), ShulkServerUi.px(context, 64)));
			LinearLayout copy = new LinearLayout(context); copy.setOrientation(LinearLayout.VERTICAL); copy.setGravity(Gravity.CENTER_VERTICAL); copy.setPadding(ShulkServerUi.px(context, 16), 0, 0, 0);
			TextView name = ShulkServerUi.text(context, data.name, 24, 0xFFF1ECEF); name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
			detail = ShulkServerUi.text(context, data.ip, 15, 0xFF817984); detail.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
			motd = ShulkServerUi.text(context, "Pinging server description…", 13, 0xFF9F96A4); motd.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
			copy.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ShulkServerUi.px(context, 32)));
			copy.addView(detail, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ShulkServerUi.px(context, 24)));
			copy.addView(motd, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ShulkServerUi.px(context, 22)));
			view.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
			view.setOnClickListener(ignored -> select(this));
		}
	}
}
