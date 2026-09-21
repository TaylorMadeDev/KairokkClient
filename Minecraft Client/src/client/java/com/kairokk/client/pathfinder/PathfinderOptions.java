package com.kairokk.client.pathfinder;

/** Pathfinder controls. Values never modify the player's server-owned attributes. */
public final class PathfinderOptions {
	private PathfinderOptions() {}
	public static boolean smartJump=true, avoidLava=true, safeWalk=true, saferRoutes=true, avoidWater=true;
	public static boolean recalculateOnStuck=true, pauseInGui=true, clearOnDisable=true, autoFollow=false;
	public static boolean scanEffects=true, refreshEffects=true, thirdPerson=true, glow=true, fade=true;
	public static float walkSpeed=1f, jumpMargin=.35f, stepHeight=.65f, maxFall=1f;
	public static int renderDistance=64, updateRate=10, stuckTimeout=5;
	public static float lineWidth=2f, nodeSize=1f;
	public static int pathColor=0xFF3B82F6, nodeColor=0xFF22D3EE, targetColor=0xFF10B981, invalidColor=0xFFEF4444;
	public static String movementMode="Balanced", renderMode="Full", lineStyle="Dashed", nodeStyle="Cube";
	public static String preset="Balanced";
	private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("kairokk-pathfinder");
	private static final java.nio.file.Path CONFIG=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("kairokk-pathfinder.properties");
	static {
		if(java.nio.file.Files.exists(CONFIG))try(var input=java.nio.file.Files.newInputStream(CONFIG)){
			java.util.Properties properties=new java.util.Properties();properties.load(input);
			PathFinderSettings.sprintJump=Boolean.parseBoolean(properties.getProperty("sprintJump","true"));
			for(var field:PathfinderOptions.class.getFields()){String value=properties.getProperty(field.getName());if(value==null)continue;try{
				if(field.getType()==boolean.class)field.setBoolean(null,Boolean.parseBoolean(value));
				else if(field.getType()==int.class)field.setInt(null,Integer.parseInt(value));
				else if(field.getType()==float.class){float parsed=Float.parseFloat(value);if(Float.isFinite(parsed))field.setFloat(null,parsed);}
				else if(field.getType()==String.class)field.set(null,value);
			}catch(ReflectiveOperationException|IllegalArgumentException invalid){LOG.warn("Invalid Pathfinder option {}",field.getName());}}
			walkSpeed=Math.clamp(walkSpeed,.25f,1f);updateRate=Math.clamp(updateRate,1,20);renderDistance=Math.clamp(renderDistance,16,128);maxFall=Math.clamp(maxFall,1f,5f);stuckTimeout=Math.clamp(stuckTimeout,2,10);
		}catch(java.io.IOException exception){LOG.warn("Could not load Pathfinder options",exception);}
	}
	public static synchronized void save(){
		java.util.Properties properties=new java.util.Properties();
		properties.setProperty("sprintJump",Boolean.toString(PathFinderSettings.sprintJump));
		try{for(var field:PathfinderOptions.class.getFields())properties.setProperty(field.getName(),String.valueOf(field.get(null)));
			java.nio.file.Files.createDirectories(CONFIG.getParent());try(var output=java.nio.file.Files.newOutputStream(CONFIG)){properties.store(output,"Kairokk Pathfinder");}
		}catch(java.io.IOException|IllegalAccessException exception){LOG.warn("Could not save Pathfinder options",exception);}
	}
	public static void applyPreset(String value){
		preset=value;
		switch(value){
			case "Safe" -> { PathFinderSettings.sprintJump=false; smartJump=false; avoidLava=true; safeWalk=true; saferRoutes=true; avoidWater=true; movementMode="Careful"; walkSpeed=.72f; jumpMargin=.55f; maxFall=1f; }
			case "Fast" -> { PathFinderSettings.sprintJump=true; smartJump=true; avoidLava=true; safeWalk=false; saferRoutes=false; avoidWater=false; movementMode="Fast"; walkSpeed=1f; jumpMargin=.25f; maxFall=3f; }
			case "Minimal" -> { PathFinderSettings.sprintJump=false; smartJump=false; avoidLava=false; safeWalk=false; saferRoutes=false; avoidWater=false; movementMode="Balanced"; walkSpeed=1f; jumpMargin=.35f; maxFall=2f; }
			case "Custom" -> { }
			default -> { PathFinderSettings.sprintJump=true; smartJump=true; avoidLava=true; safeWalk=true; saferRoutes=true; avoidWater=true; movementMode="Balanced"; walkSpeed=1f; jumpMargin=.35f; maxFall=1f; }
		}
		save();
	}
}
