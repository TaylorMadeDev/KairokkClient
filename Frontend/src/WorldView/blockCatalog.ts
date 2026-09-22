export type BlockVisual = { color: number; shape: 'box' | 'cross'; width: number; height: number; depth: number; transparent: boolean; opacity: number; opaque: boolean }

const cache = new Map<string, BlockVisual>()
const namedColors: [RegExp, number][] = [
  [/lily_pad/, 0x357d36], [/cactus/, 0x397f37], [/water/, 0x2477bb], [/lava/, 0xe96a13],
  [/sandstone/, 0xc7af75], [/sand/, 0xcbb77d], [/red_nether_brick/, 0x501f26], [/nether_brick/, 0x382328],
  [/deepslate|blackstone|basalt/, 0x41464b], [/stone|cobblestone|andesite|gravel/, 0x777d80],
  [/diorite|quartz|calcite/, 0xd2d0c7], [/granite|terracotta|brick/, 0x9b6152],
  [/mud|dirt|farmland|podzol/, 0x79543a], [/grass_block|moss|^grass$|^plant$|flower|fern/, 0x4f8d3c],
  [/leaves|vine/, 0x357d43], [/log|wood|planks|bamboo|oak|spruce|birch|jungle|acacia|cherry|mangrove/, 0x89683e],
  [/glass|ice/, 0x9ccee0], [/chest|barrel/, 0xa47837], [/portal|amethyst/, 0x8646d9],
  [/gold|yellow|light_weighted_pressure_plate/, 0xe2bf43], [/iron|light_gray|heavy_weighted_pressure_plate/, 0xb9c0c5], [/copper|orange/, 0xb8734b],
  [/diamond|cyan/, 0x51b7b3], [/emerald|lime/, 0x55ac69], [/redstone|red/, 0xb84d48],
  [/blue/, 0x477bb3], [/green/, 0x4f8d3c], [/brown/, 0x79543a], [/purple|magenta/, 0x9160ab], [/pink/, 0xc98da8],
  [/black/, 0x35383e], [/white|snow/, 0xe5e7e2], [/gray/, 0x777c83],
]

const fallbackColor = (id: string) => {
  let hash = 0
  for (const character of id) hash = Math.imul(hash, 31) + character.charCodeAt(0) | 0
  const hue = ((hash >>> 0) % 360) / 360
  const channel = (phase: number) => Math.round((.47 + .14 * Math.cos((hue + phase) * Math.PI * 2)) * 255)
  return (channel(0) << 16) | (channel(1 / 3) << 8) | channel(2 / 3)
}

export function blockVisual(kind: string): BlockVisual {
  const saved = cache.get(kind)
  if (saved) return saved
  const id = kind.split(':').pop() ?? kind
  const color = namedColors.find(([pattern]) => pattern.test(id))?.[1] ?? fallbackColor(id)
  let shape: BlockVisual['shape'] = 'box', width = 1, height = 1, depth = 1
  let transparent = false, opacity = 1, opaque = true
  if (id === 'plant' || kind !== 'grass' && /(^|_)(flower|sapling|fern|grass|seagrass|bush|mushroom|crop|tulip|orchid|dandelion|poppy|rose|lily_of_the_valley|sugar_cane)$/.test(id)) {
    shape = 'cross'; width = depth = .86; height = .92; opaque = false
  } else if (id === 'lily_pad') {
    width = depth = .94; height = .07; opaque = false
  } else if (id.endsWith('_carpet') || id === 'moss_carpet') {
    height = .063; opaque = false
  } else if (id.endsWith('_pressure_plate')) {
    width = depth = .75; height = .063; opaque = false
  } else if (id === 'cactus') {
    width = depth = .875; opaque = false
  } else if (id.endsWith('_slab')) {
    height = .5; opaque = false
  } else if (id.endsWith('_stairs')) {
    height = .75; opaque = false
  } else if (id.endsWith('_fence') || id.endsWith('_wall')) {
    width = depth = .35; opaque = false
  } else if (id.endsWith('_pane') || id.endsWith('_bars')) {
    width = depth = .16; opaque = false
  } else if (id === 'water' || id === 'lava') {
    height = .88; transparent = true; opacity = id === 'water' ? .68 : .82; opaque = false
  } else if (id === 'leaves' || id.endsWith('_leaves') || id.includes('glass') || id.includes('ice')) {
    transparent = true; opacity = .86; opaque = false
  }
  const visual = { color, shape, width, height, depth, transparent, opacity, opaque }
  cache.set(kind, visual)
  return visual
}
