package com.kairokk.realtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kairokk.client.pathfinder.pathfinding.NavigationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/** Subscription-driven, ephemeral world collector. Minecraft objects are read only on the client thread. */
public final class WorldViewService {
	private static final int MAX_RADIUS = 8, DEFAULT_RADIUS = 4, MAX_ENCODER_QUEUE = 24;
	private final BiConsumer<String, JsonObject> sender;
	private final ExecutorService encoder = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "kairokk-world-encoder"); t.setDaemon(true); return t; });
	private final ArrayBlockingQueue<ChunkSnapshot> encodeQueue = new ArrayBlockingQueue<>(MAX_ENCODER_QUEUE);
	private final ArrayDeque<ChunkCoordinate> pending = new ArrayDeque<>();
	private final Set<ChunkCoordinate> synchronizedChunks = ConcurrentHashMap.newKeySet();
	private final Map<String, EntitySnapshot> synchronizedEntities = new HashMap<>();
	private boolean subscribed; private int radius = DEFAULT_RADIUS, ticks; private volatile int generation; private String dimension; private int centerX = Integer.MIN_VALUE, centerZ = Integer.MIN_VALUE, lastPathHash;

	public WorldViewService(BiConsumer<String, JsonObject> sender) { this.sender = sender; encoder.execute(this::encodeLoop); }
	public void subscribe(int requestedRadius) { radius = Math.max(1, Math.min(MAX_RADIUS, requestedRadius)); subscribed = true; reset("SUBSCRIBED"); }
	public void unsubscribe() { subscribed = false; clear(); }
	public void disconnected() { subscribed = false; clear(); }
	public boolean active() { return subscribed; }

	public void tick(Minecraft client) {
		if (!subscribed) return;
		if (client.level == null || client.player == null) { clear(); return; }
		String currentDimension = client.level.dimension().identifier().toString();
		if (!currentDimension.equals(dimension)) { dimension = currentDimension; reset("DIMENSION_CHANGED"); }
		int cx = client.player.blockPosition().getX() >> 4, cz = client.player.blockPosition().getZ() >> 4;
		if (cx != centerX || cz != centerZ) updateWindow(client, cx, cz);
		if (!pending.isEmpty() && encodeQueue.remainingCapacity() > 0) capture(client, pending.removeFirst());
		ticks++; if (ticks % 3 == 0) sendPlayer(client); if (ticks % 5 == 0) sendEntities(client); if (ticks % 20 == 0) { sendMetadata(client); sendPath(); }
	}

	private void reset(String reason) {
		clear(); if (!subscribed) return; Minecraft client = Minecraft.getInstance(); if (client.level == null || client.player == null) return;
		int initialCenterX = client.player.blockPosition().getX() >> 4, initialCenterZ = client.player.blockPosition().getZ() >> 4;
		dimension = client.level.dimension().identifier().toString(); JsonObject init = new JsonObject(); init.addProperty("sessionId", UUID.randomUUID().toString()); init.addProperty("expectedChunks", (radius * 2 + 1) * (radius * 2 + 1)); init.addProperty("centerX", initialCenterX); init.addProperty("centerZ", initialCenterZ); init.add("metadata", metadata(client)); sender.accept("WORLD_INIT", init); updateWindow(client, initialCenterX, initialCenterZ);
	}

	private void clear() { generation++; pending.clear(); encodeQueue.clear(); synchronizedChunks.clear(); synchronizedEntities.clear(); centerX = Integer.MIN_VALUE; centerZ = Integer.MIN_VALUE; dimension = null; }
	private void updateWindow(Minecraft client, int cx, int cz) {
		centerX = cx; centerZ = cz; Set<ChunkCoordinate> wanted = new java.util.HashSet<>(); for (int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) wanted.add(new ChunkCoordinate(cx+dx,cz+dz));
		for (ChunkCoordinate old : Set.copyOf(synchronizedChunks)) if (!wanted.contains(old)) { synchronizedChunks.remove(old); JsonObject data=new JsonObject();data.addProperty("key",old.key());sender.accept("WORLD_CHUNK_UNLOAD",data); }
		pending.removeIf(coordinate -> !wanted.contains(coordinate)); wanted.stream().filter(coordinate -> !synchronizedChunks.contains(coordinate) && !pending.contains(coordinate)).sorted(Comparator.comparingInt(coordinate -> coordinate.distanceSquared(cx,cz))).forEach(pending::addLast);
	}

	private void capture(Minecraft client, ChunkCoordinate coordinate) {
		if (client.level == null || !client.level.hasChunk(coordinate.x, coordinate.z)) { pending.addLast(coordinate); return; }
		JsonArray blocks = new JsonArray();
		for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
			int x = (coordinate.x << 4) + lx, z = (coordinate.z << 4) + lz;
			int canopyTop = client.level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
			int scanFloor = Math.max(client.level.getMinY(), canopyTop - 48), terrainY = scanFloor;
			for (int y = canopyTop - 1; y >= scanFloor; y--) {
				BlockState candidate = client.level.getBlockState(new BlockPos(x, y, z));
				if (candidate.isAir()) continue;
				String kind = classify(candidate);
				if (!kind.equals("leaves") && !kind.equals("wood") && !kind.equals("plant") && !kind.equals("water")) { terrainY = y; break; }
			}
			int bottom = Math.max(client.level.getMinY(), terrainY - 3);
			for (int y = bottom; y < canopyTop; y++) {
				BlockState state = client.level.getBlockState(new BlockPos(x, y, z));
				if (state.isAir()) continue;
				JsonObject block = new JsonObject(); block.addProperty("x", x); block.addProperty("y", y); block.addProperty("z", z); block.addProperty("kind", classify(state)); blocks.add(block);
			}
		}
		if (!encodeQueue.offer(new ChunkSnapshot(generation, coordinate, blocks))) pending.addFirst(coordinate);
	}

	private void encodeLoop() { while (!Thread.currentThread().isInterrupted()) try { ChunkSnapshot snapshot=encodeQueue.take(); if(snapshot.generation!=generation)continue; JsonObject chunk=new JsonObject();chunk.addProperty("key",snapshot.coordinate.key());chunk.addProperty("x",snapshot.coordinate.x);chunk.addProperty("z",snapshot.coordinate.z);chunk.addProperty("revision",System.nanoTime());chunk.add("blocks",snapshot.blocks);JsonObject data=new JsonObject();data.add("chunk",chunk);if(snapshot.generation!=generation)continue;sender.accept("WORLD_CHUNK",data);if(snapshot.generation==generation)synchronizedChunks.add(snapshot.coordinate); } catch (InterruptedException stop) { Thread.currentThread().interrupt(); } }

	private void sendPlayer(Minecraft client) { var p=client.player; if(p==null)return; var velocity=p.getDeltaMovement(); JsonObject player=new JsonObject();player.addProperty("uuid",p.getUUID().toString());player.addProperty("name",p.getName().getString());player.addProperty("x",p.getX());player.addProperty("y",p.getY());player.addProperty("z",p.getZ());player.addProperty("yaw",p.getYRot());player.addProperty("pitch",p.getXRot());JsonObject motion=new JsonObject();motion.addProperty("x",velocity.x);motion.addProperty("y",velocity.y);motion.addProperty("z",velocity.z);player.add("velocity",motion);player.addProperty("onGround",p.onGround());player.addProperty("sprinting",p.isSprinting());player.addProperty("sneaking",p.isCrouching());player.addProperty("usingItem",p.isUsingItem());player.addProperty("swimming",p.isSwimming());player.addProperty("fallFlying",p.isFallFlying());JsonObject data=new JsonObject();data.add("player",player);sender.accept("WORLD_PLAYER_STATE",data); }
	private void sendEntities(Minecraft client) {
		if (client.level == null || client.player == null) return;
		double range = radius * 16.0 + 16.0, rangeSquared = range * range;
		Map<String, EntitySnapshot> current = new HashMap<>();
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity == client.player || entity.distanceToSqr(client.player) > rangeSquared) continue;
			String id = entity.getUUID().toString();
			String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
			Float health = entity instanceof LivingEntity living ? living.getHealth() : null;
			boolean hostile = entity.getType().getCategory() == MobCategory.MONSTER;
			EntitySnapshot snapshot = new EntitySnapshot(id, type, entity.getX(), entity.getY(), entity.getZ(), health, hostile);
			current.put(id, snapshot);
			EntitySnapshot previous = synchronizedEntities.get(id);
			if (previous == null) sendEntity("WORLD_ENTITY_SPAWN", snapshot);
			else if (snapshot.differsFrom(previous)) sendEntity("WORLD_ENTITY_UPDATE", snapshot);
		}
		for (String id : Set.copyOf(synchronizedEntities.keySet())) if (!current.containsKey(id)) {
			JsonObject data = new JsonObject(); data.addProperty("id", id); sender.accept("WORLD_ENTITY_REMOVE", data);
		}
		synchronizedEntities.clear(); synchronizedEntities.putAll(current);
	}
	private void sendEntity(String event, EntitySnapshot snapshot) {
		JsonObject entity = new JsonObject(); entity.addProperty("id", snapshot.id); entity.addProperty("type", snapshot.type); entity.addProperty("x", snapshot.x); entity.addProperty("y", snapshot.y); entity.addProperty("z", snapshot.z); if (snapshot.health != null) entity.addProperty("health", snapshot.health); entity.addProperty("hostile", snapshot.hostile);
		JsonObject data = new JsonObject(); data.add("entity", entity); sender.accept(event, data);
	}
	private void sendMetadata(Minecraft client) { JsonObject data=new JsonObject();data.add("metadata",metadata(client));sender.accept("WORLD_METADATA",data); }
	private JsonObject metadata(Minecraft client) { JsonObject value=new JsonObject();value.addProperty("dimension",client.level==null?"unknown":client.level.dimension().identifier().toString());value.addProperty("biome","Local biome");value.addProperty("facing",client.player==null?"Unknown":client.player.getDirection().getName());value.addProperty("light",client.level==null||client.player==null?0:client.level.getMaxLocalRawBrightness(client.player.blockPosition()));value.addProperty("time",client.level==null?"Unknown":Long.toString(client.level.getGameTime()));value.addProperty("expectedChunks",(radius*2+1)*(radius*2+1));return value; }
	private void sendPath() { var path=NavigationManager.getInstance().currentPath();int hash=path==null?0:path.points().hashCode();if(hash==lastPathHash)return;lastPathHash=hash;JsonArray nodes=new JsonArray();if(path!=null)for(var point:path.points()){JsonObject node=new JsonObject();node.addProperty("x",point.position().x);node.addProperty("y",point.position().y);node.addProperty("z",point.position().z);nodes.add(node);}JsonObject data=new JsonObject();data.add("nodes",nodes);sender.accept("PATHFINDER_PATH",data); }
	private static String classify(BlockState state) {
		String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		if (id.contains("water")) return "water";
		if (id.contains("leaves")) return "leaves";
		if (id.equals("grass") || id.equals("short_grass") || id.equals("tall_grass") || id.equals("seagrass") || id.equals("tall_seagrass") || id.contains("fern") || id.endsWith("_flower") || id.endsWith("_sapling") || id.endsWith("_bush")) return "plant";
		if (id.contains("log") || id.contains("wood")) return "wood";
		if (id.contains("sand")) return "sand";
		if (id.contains("grass") || id.contains("moss")) return "grass";
		if (id.contains("dirt") || id.contains("mud")) return "dirt";
		if (id.contains("portal")) return "portal";
		if (id.contains("chest") || id.contains("barrel")) return "container";
		return "stone";
	}
	private record ChunkCoordinate(int x,int z){String key(){return x+","+z;}int distanceSquared(int ox,int oz){int dx=x-ox,dz=z-oz;return dx*dx+dz*dz;}}
	private record ChunkSnapshot(int generation,ChunkCoordinate coordinate,JsonArray blocks){}
	private record EntitySnapshot(String id, String type, double x, double y, double z, Float health, boolean hostile) {
		boolean differsFrom(EntitySnapshot previous) { return Math.abs(x - previous.x) > .02 || Math.abs(y - previous.y) > .02 || Math.abs(z - previous.z) > .02 || !java.util.Objects.equals(health, previous.health) || hostile != previous.hostile || !type.equals(previous.type); }
	}
}
