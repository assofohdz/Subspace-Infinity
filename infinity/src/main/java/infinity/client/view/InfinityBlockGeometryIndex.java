/*
 * $Id$
 *
 * Copyright (c) 2017, Simsilica, LLC
 * All rights reserved.
 */
package infinity.client.view;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.jme3.asset.AssetManager;
import com.simsilica.mblock.*;
import infinity.systems.MapSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.util.BufferUtils;

import com.simsilica.mblock.geom.BlockFactory;
import com.simsilica.mblock.geom.DefaultPartBuffer;
import com.simsilica.mblock.geom.GeomPart;
import com.simsilica.mblock.geom.MaterialType;

import infinity.map.LevelFile;
import infinity.map.LevelLoader;

/**
 *
 *
 * @author Paul Speed
 */
public class InfinityBlockGeometryIndex extends BlockGeometryIndex{

    private static int TILEID_MASK = 0x000000ff;
    private static int MAPID_MASK = 0x000fff00;

    // private static String INVISIBLE = "invisible";
    // private static String TILE = "tile";

    public static int BOTTOM_TILE_LAYER = 1;
    public static int TOP_INVISIBLE_LAYER = 2;

    static Logger log = LoggerFactory.getLogger(InfinityBlockGeometryIndex.class);

    private final Map<Integer, Material> materials = new HashMap<>();

    // private MaterialType[] materialTypes;
    // private BlockType[] types;
    private final Map<Integer, BlockType> tileKeyToBlockTypeMap = new HashMap<>();
    private final Map<Integer, MaterialType> tileKeyToMaterialType = new HashMap<>();

    public static boolean debug = false;
    // private boolean logged;
    private final AWTLoader imgLoader;

    private final Map<Integer, LevelFile> mapIdToLevels = new HashMap<>();

    private DesktopAssetManager am;

    // Map from tilekey to image
    private final HashMap<Integer, Image> tileKeyToImageMap = new HashMap<>();

    public InfinityBlockGeometryIndex(AssetManager am) {
        super(am);
        this.am = new DesktopAssetManager(true);
        this.am.registerLoader(LevelLoader.class, "lvl");
        imgLoader = new AWTLoader();

        // Just hard code some stuff for now
        /*
         * this.materialTypes = new MaterialType[]{ null, new MaterialType("palette1",
         * 1, true, false, false), new MaterialType("tunnelbase.lvl", 1, true, false,
         * false) };
         */
        /*
         * this.types = new BlockType[257]; for (int i = 0; i < 64; i++) { types[i + 1]
         * = new BlockType(createFactory(1, i)); }
         */
    }

    private BlockFactory createFactory(final int tileKey, @SuppressWarnings("unused") final int color) {
        // final int x = color & 0x7;
        // final int y = (color & 0x38) >> 3;

        // final float textureScale = 0.125f; // 1/8th
        // final float halfScale = textureScale * 0.5f;
        // final float s = x * textureScale + halfScale;
        // final float t = 1f - (y * textureScale + halfScale);
        // float t = y * 0.125f;

        // log.info("createFactory:: tileKey = "+tileKey);
        final InfinityBlockFactory result = InfinityBlockFactory.createCube(0, getMaterialType(tileKey));
        /*
         * for (PartFactory partFactory : result.getDirParts()) { for (GeomPart part :
         * ((DefaultPartFactory) partFactory).getTemplates()) {
         *
         * // Convenient that we want all texture coordinates to be the same float[]
         * texes = part.getTexCoords(); for (int i = 0; i < texes.length; i += 2) {
         * texes[i] = s; texes[i + 1] = t; } } }
         */
        return result;
    }

    private BlockType getBlockType(final int tileKey) {
        BlockType type = tileKeyToBlockTypeMap.get(Integer.valueOf(tileKey));

        if (type == null) {
            type = new BlockType(new BlockName("", ""), createFactory(tileKey, 1));

            tileKeyToBlockTypeMap.put(Integer.valueOf(tileKey), type);
        } else {
            // log.info("Found cached BlockType: "+type);
        }
        return type;
    }

    // First type of information:
    private BlockType getBlockType(final int tileId, final int mapId) {

        // Check to see if we have loaded this map before
        if (!mapIdToLevels.containsKey(Integer.valueOf(mapId))) {
            // TODO: Lookup stringname based on mapId
            //For now, use same mapname as server side
            //final LevelFile level = loadMap(MapSystem.MAPNAME);
            //mapIdToLevels.put(Integer.valueOf(mapId), level);
        }

        final int tileKey = tileId | (mapId << 8);
        // log.info("getBlockType:: tileKey = " + tileKey + " <= (Tile,Map) = (" +
        // tileId + "," + mapId + ")");

        return getBlockType(tileKey);
    }

    // Second type of info:
    private MaterialType getMaterialType(final int tileKey) {

        MaterialType matType = tileKeyToMaterialType.get(Integer.valueOf(tileKey));

        if (matType == null) {

            // final int tileId = tileKey & TILEID_MASK;
            // final int mapId = (tileKey & MAPID_MASK) >> 8;
            // TODO: Lookup the levelname, using the id:

            // log.info("getMaterialType:: tileKey = " + tileKey + " => (Tile,Map) = (" +
            // tileId + "," + mapId + ")");

            // final String mapName = "aswz.lvl";

            // matType = new MaterialType("",tileKey);
            matType = new MaterialType(String.valueOf(tileKey), false, false, false);

            tileKeyToMaterialType.put(Integer.valueOf(tileKey), matType);
        }

        return matType;
    }

    // Third type of information:
    private Material getMaterial(final int tileKey) {
        // int tileKey = tileId | (mapId << 16);
        final int tileId = tileKey & TILEID_MASK;
        final int mapId = (tileKey & MAPID_MASK) >> 8;

        Material mat = materials.get(Integer.valueOf(tileKey));

        if (mat == null) {
            mat = new Material(am, "MatDefs/BlackTransparentShader.j3md");

            // int key = tileIndex | (mapId << 16);
            Image jmeOutputImage = tileKeyToImageMap.get(Integer.valueOf(tileKey));
            if (jmeOutputImage == null) {
                final java.awt.Image awtInputImage = mapIdToLevels.get(Integer.valueOf(mapId)).getTiles()[tileId - 1];
                jmeOutputImage = imgLoader.load(toBufferedImage(awtInputImage), true);

                tileKeyToImageMap.put(Integer.valueOf(tileKey), jmeOutputImage);
                // log.info("Put tile: "+tileIndex+" image into map");
            }
            final Texture2D tex2D = new Texture2D(jmeOutputImage);
            mat.setTexture("ColorMap", tex2D);
            // mat = globals.createMaterial(texture, false).getMaterial();
            materials.put(Integer.valueOf(tileKey), mat);
            jmeOutputImage.dispose();
        } else {
            // log.info("Found cached material: "+mat+" for tileKey = " + tileKey + " <=
            // (Tile,Map) = (" + tileId + "," + mapId + ")");
        }
        return mat;
    }

    public Node generateBlocks(final Node target, final CellArray cells, final CellData lightData,
                               final boolean smoothLighting) {

        return geomFactory.generateBlocks(target, cells, lightData, smoothLighting);
        /*
        final Set<Integer> tileSet = new HashSet<>();
        // int count = 0;
        // final int count2 = 0;
//System.out.println("===============generateBlocks(" + target + ", " + cells + ")");
        // final long start = System.nanoTime();
        final Node result = target;
        result.detachAllChildren();

        final DefaultPartBuffer buffer = new DefaultPartBuffer();

        final int xSize = cells.getSizeX();
        final int ySize = cells.getSizeY();
        final int zSize = cells.getSizeZ();

        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    final int val = cells.getCell(x, y, z);
                    // count++;
//System.out.println("[" + x + "][" + y + "][" + z + "] val:" + val);
                    final int tileId = MaskUtils.getType(val) & 0x000000ff;

                    if (tileId == 0) {
                        // log.info("TileID == 0 for val = " + val + " => (Tile) = (" + tileId + ") -
                        // Coords: ["+x+", "+y+", "+z+"]");
                        continue;
                    }
                    tileSet.add(Integer.valueOf(tileId));

                    final int mapId = (MaskUtils.getType(val) & 0x000fff00) >> 8;

                    // final Vector3f targetLoc = target.getWorldTranslation();
                    // final Vector3f worldLoc = targetLoc.add(x, y, z);
                    // log.info("generateBlocks:: val = " + val + " => (Tile,Map) = (" + tileId +
                    // "," + mapId + ") - Coords: ["+worldLoc+"]");

                    final BlockType blockType = getBlockType(tileId, mapId);

                    if (blockType == null) {
                        // log.info("BlockType was null for val = " + val + " => (Tile,Map) = (" +
                        // tileId + "," + mapId + ") - Coords: ["+x+", "+y+", "+z+"]");
                        continue;
                    }
                    // No masks for now so we'll force it
                    final int lightMask = 0;
                    final int sideMask = MaskUtils.getSideMask(val);
                    if (debug && sideMask != 0) {
                        log.info("[" + x + "][" + y + "][" + z + "] val:" + val + " @" + tileId + " #"
                                + Integer.toBinaryString(sideMask));
                    }
                    InfinityBlockFactory.debug = debug;

                    // blockType.getFactory().addGeometryToBuffer(buffer, x, y, z, x, y, z,
                    // sideMask, lightMask, blockType);
                    blockType.getFactory().addGeometryToBuffer(buffer, x, y, z, x, y, z, sideMask, cells, blockType);
                }
            }
        }
        // log.info("Counted: " + count2 + " cells with value != 0");
        // log.info("Counted: " + tileSet.size() + " different tiles");

        for (final DefaultPartBuffer.PartList list : buffer.getPartLists()) {
//            System.out.println("Part list:" + list);

            if (list.list.isEmpty()) {
                // log.info("List.list was empty for buffer: " + buffer.toString() + " and List:
                // " + list);
                continue;
            }

            // We know we have simplified geometry so our mesh generation
            // can also be simplified
            final int vertCount = list.vertCount;
            final FloatBuffer pos = BufferUtils.createFloatBuffer(vertCount * 3);
            // ByteBuffer texes = BufferUtils.createByteBuffer(vertCount * 2);
            final FloatBuffer texes = BufferUtils.createFloatBuffer(vertCount * 2);
            final ShortBuffer indexes = BufferUtils.createShortBuffer(list.triCount * 3);
            // ByteBuffer normalIndexes = BufferUtils.createByteBuffer(vertCount);

            // We'll create real normals for now to avoid a custom material
            final FloatBuffer norms = BufferUtils.createFloatBuffer(vertCount * 3);

            int baseIndex = 0;
            for (final DefaultPartBuffer.PartEntry entry : list.list) {

                final int i = entry.i;
                final int j = entry.j;
                final int k = entry.k;
                final GeomPart part = entry.part;

                final int dir = part.getDirection();
//System.out.println("dir:" + dir);
                final int size = part.getVertexCount();
                final float[] verts = part.getCoords();
                int vIndex = 0;
                // final int tIndex = 0;
                for (int v = 0; v < size; v++) {
                    final float x = verts[vIndex++];
                    final float y = verts[vIndex++];
                    final float z = verts[vIndex++];
//System.out.println("pos:" + (i + x) + ", " + (j + y) + ", " + (k + z));
                    pos.put(i + x);
                    pos.put(j + y);
                    pos.put(k + z);

                    switch (dir) {
                    case 0:
                        norms.put(0).put(0).put(-1);
                        break;
                    case 1:
                        norms.put(0).put(0).put(1);
                        break;
                    case 2:
                        norms.put(1).put(0).put(0);
                        break;
                    case 3:
                        norms.put(-1).put(0).put(0);
                        break;
                    case 4:
                        norms.put(0).put(1).put(0);
                        break;
                    default:
                    case 5:
                        norms.put(0).put(-1).put(0);
                        break;
                    }
                }

                final float[] texArray = part.getTexCoords();
                for (final float element : texArray) {
                    if (element < 0 || element > 1) {
                        throw new RuntimeException(
                                "Entry has out of bounds texcoord:" + element + " type:" + part.getMaterialType());
                    }
                    // texes.put((byte)(texArray[t] * 255));
                    texes.put(element);
                }

                // The indexes need to be offset also
                for (final short s : part.getIndexes()) {
                    indexes.put((short) (baseIndex + s));
                }
                baseIndex += size;
            }
//System.out.println("Index count:" + baseIndex);

            final Mesh mesh = new Mesh();
            mesh.setBuffer(VertexBuffer.Type.Position, 3, pos);
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texes);
            mesh.setBuffer(VertexBuffer.Type.Index, 3, indexes);
            mesh.setBuffer(VertexBuffer.Type.Normal, 3, norms);
            mesh.setStatic();
            mesh.updateBound();

            final Quad quad = new Quad(1, 1);
            quad.setBuffer(VertexBuffer.Type.Position, 3, pos);
            quad.setBuffer(VertexBuffer.Type.Index, 3, indexes);
            quad.setBuffer(VertexBuffer.Type.Normal, 3, norms);
            quad.setBuffer(VertexBuffer.Type.TexCoord, 2, texes);
            quad.setStatic();
            quad.updateBound();

            // Geometry geom = new Geometry("mesh:" + list.materialType + ":" +
            // list.primitiveType, mesh);
            final Geometry geom = new Geometry("mesh:" + list.materialType + ":" + list.primitiveType, quad);

            // geom.setMaterial(getMaterial(list.materialType.getId()));
            String id = list.materialType.getId();

            geom.setMaterial(getMaterial(Integer.valueOf(id)));

            result.attachChild(geom);
            // count++;
        }

        // log.info("Counted: " + count + " meshes added to world");
        // final long end = System.nanoTime();
//        System.out.println("Generated in:" + ((end - start)/1000000.0) + " ms");
        return result;

        */
    }

    // Subspace infinity version, we don't want to re-calculate below or above the
    // layer
    public static void recalculateSideMasks(final CellData data, final int x, final int y, final int z) {
        int currX = x;
        int currY = y;
        int currZ = z;
        final int xStart = currX;
        final int yStart = Math.max(0, currY); // y is 0 to infinity
        final int zStart = currZ;
        final int xEnd = currX;
        final int yEnd = currY;
        final int zEnd = currZ;

        for (currX = xStart; currX <= xEnd; currX++) {
            for (currY = yStart; currY <= yEnd; currY++) {
                for (currZ = zStart; currZ <= zEnd; currZ++) {
                    final int val = data.getCell(currX, currY, currZ);
                    final int tileId = MaskUtils.getType(val);
//log.info("  [" + x + "][" + y + "][" + z + "] = " + val + " @" + type + " #" + Integer.toBinaryString(getSideMask(val)));
                    // if( type == 0 ) {
                    // // 0 is always empty space
                    // continue;
                    // }
                    // But we can't skip it completely because the existing
                    // mask might still be wrong.
                    /*
                     * BlockType type = types[val]; if( type == null ) { continue; }
                     */
                    int sideMask = 0;

                    // 0 is always empty space so we only need to recalculate
                    // a mask if there is a thing there... but we still want to
                    // set an empty mask back in case it wasn't empty before.
                    // Note: this shouldn't happen in practice because of how we
                    // set the cell data to the raw type before applying side masks
                    // but it's better to be correct just in case... and costs us
                    // nothing.
                    if (tileId != 0) {
                        // Calculate the mask right here
                        for (final Direction dir : Direction.values()) {
                            final int next = MaskUtils.getType(data.getCell(currX, currY, currZ, dir, 0));
//log.info("    " + dir + " -> " + next);
                            // Just a simple check for now
                            if (next == 0) {
                                // It's empty so we emit a face in that direction
                                sideMask = sideMask | dir.getBitMask();
                            }
                        }
                    }
//log.info("    result:" + setSideMask(val, sideMask) + "   sides:" + Integer.toBinaryString(sideMask));
                    data.setCell(currX, currY, currZ, MaskUtils.setSideMask(val, sideMask));
                }
            }
        }

    }

    protected LevelFile loadMap(final String tileSet) {
        final LevelFile localMap = (LevelFile) am.loadAsset(tileSet);

        return localMap;
    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    private BufferedImage toBufferedImage(final java.awt.Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        final int width = img.getWidth(null);
        final int height = img.getHeight(null);

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        final Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null); // No flip
        // bGr.drawImage(img, 0 + width, 0, -width, height, null); //Horisontal flip
        // bGr.drawImage(img, 0, 0 + height, width, -height, null); //Vertical flip
        // bGr.drawImage(img, height, 0, -width, height, null);

        bGr.dispose();

        final AffineTransform tx = AffineTransform.getScaleInstance(-11, -1);
        tx.translate(-bimage.getWidth(null), -bimage.getHeight(null));
        final AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        bimage = op.filter(bimage, null);

        // Return the buffered image
        return bimage;
    }

    /**
     * This array is used to define the quad bounds in the right order. Its
     * important relative to where the camera is and what facing the camera has
     *
     * @param halfSize
     * @return array
     */
    @SuppressWarnings("unused")
    private float[] getVertices(final float halfSize) {
        final float[] res = new float[] { halfSize, 0, -halfSize, -halfSize, 0, -halfSize, -halfSize, 0, halfSize,
                halfSize, 0, halfSize };
        return res;
    }

    /**
     * This will create the normals that is point in the z unit vector direction.
     * This is used in relation to the lighting on the quad (towards camera)
     *
     * @return float array containing the right normals
     */
    @SuppressWarnings("unused")
    private float[] getNormals() {
        float[] normals;
        normals = new float[] { 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0 };
        return normals;
    }
}
