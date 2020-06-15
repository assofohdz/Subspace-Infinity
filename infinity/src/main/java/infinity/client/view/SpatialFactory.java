/*
 * $Id$
 * 
 * Copyright (c) 2020, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package infinity.client.view;

import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.*;
import com.jme3.scene.shape.*;
import com.jme3.texture.*;

import com.simsilica.es.*;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.mathd.*;

import com.simsilica.mphys.*;
import com.simsilica.mblock.phys.*;
import com.simsilica.ext.mphys.*;

/**
 *  Convenience methods for creating Spatials from
 *  shapes.
 *  
 *  @author    Paul Speed
 */
public class SpatialFactory {

    private BlockGeometryIndex geomIndex = new BlockGeometryIndex();

    public SpatialFactory() {
    }

    public Spatial createModel( EntityId id, MBlockShape shape, Mass mass ) {
    
        Part part = shape.getPart();
        
        Spatial result;
        if( part instanceof CellArrayPart ) {
            result = createPartSpatial(id, (CellArrayPart)part, true, mass);
        } else if( part instanceof Group ) {
            result = createPartSpatial(id, (Group)part, mass);
        } else {
            throw new IllegalArgumentException("Unhandled part type:" + shape.getPart());
        }
        return result;
    }
 
    /**
     *  Creates a root-level spatial for the specified root group.
     */   
    protected Spatial createPartSpatial( EntityId id, Group group, Mass mass ) {
        Node node = new Node("Object:" + id);
        // For debugging cog... but we might as well use the non-networked demo
        //node.attachChild(createBox(0.1f, ColorRGBA.Red));
        
        // The root level will need to be positioned relative to the rigid body
        // which is positioned at CoG relative to model space.  So we need to offset
        // negative CoG.
        Node cogOffset = new Node("CoG:" + id);
        node.attachChild(cogOffset);
        Vec3d cog = group.getMass().getCog();
        cogOffset.move((float)-cog.x, (float)-cog.y, (float)-cog.z);
        createPartSpatial(cogOffset, id, group, mass);
        
        // Maybe someday when we have all the time in the world and are sitting
        // on a beach somewhere worrying about ways to micro-optimize, we can
        // see about flattening this extra hierarchy.  Though, do note that 
        // the group hierarchy is already flat... because that's how I roll.
        
        return node;
    }

    /**
     *  Creates a child spatial for the specified child group.
     */
    protected Spatial createPartSpatial( Node parent, EntityId id, Group group, Mass mass ) {
        for( Part child : group.getChildren() ) {
            if( child instanceof CellArrayPart ) {
                Spatial ps = createPartSpatial(id, (CellArrayPart)child, false, mass);
                ps.setLocalTranslation(child.getShapeRelativePosition().toVector3f());
                ps.setLocalRotation(child.getShapeRelativeOrientation().toQuaternion());
                parent.attachChild(ps);                
            } else if( child instanceof Group ) {
                createPartSpatial(parent, id, (Group)child, mass); 
            } else {
                throw new IllegalArgumentException("Unhandled part type:" + child);
            }
        }
        return parent;
    }
 
    /**
     *  Creates a root-level spatial for the specified root part.
     */   
    protected Spatial createPartSpatial( EntityId id, CellArrayPart part, boolean isRoot, Mass mass ) {
        if( part.getCells() == null ) {
            return createSphere(id, (float)part.getMass().getRadius(), mass);   
        }
        
        Node node = new Node("Object:" + id);
        Node parts = new Node("Parts:" + id);
        node.attachChild(parts);

        geomIndex.generateBlocks(parts, part.getCells());                
 
        // If we are the root level then we'll need a cog shift
        // to match with the rigid body
        if( isRoot ) {                
            BodyMass bm = part.getMass();
                
            // The position of the object is its CoG... which means
            // we need to offset our model's origin by it.  It should
            // already be scaled and everything... just need to negate it.
            Vector3f cogOffset = bm.getCog().toVector3f().negate(); 
 
            // We need to sort out what the center should be.  Directly out of generateBlocks()
            // the geometry is all relative to the corner.   See cog-offset.txt
            parts.move(cogOffset);
        }
           
        parts.setLocalScale((float)part.getScale());
        parts.setShadowMode(ShadowMode.CastAndReceive);

        node.setUserData("oid", id.getId());    
 
        return node;       
    }

    
    public Spatial createSphere( EntityId id, float radius, Mass mass ) {
        Sphere mesh = new Sphere(24, 24, radius);
        mesh.setTextureMode(Sphere.TextureMode.Projected);
        mesh.scaleTextureCoordinates(new Vector2f(4, 2));
        Geometry geom = new Geometry("Object:" + id, mesh);
            
        if( mass != null && mass.getMass() != 0 ) { 
            geom.setMaterial(GuiGlobals.getInstance().createMaterial(new ColorRGBA(0, 0.6f, 0.6f, 1), true).getMaterial());
            
            Texture texture = GuiGlobals.getInstance().loadTexture("Interface/grid-shaded-labeled.png", true, true);
            geom.getMaterial().setTexture("DiffuseMap", texture);
        } else {
            // Just a flat green
            geom.setMaterial(GuiGlobals.getInstance().createMaterial(new ColorRGBA(0.2f, 0.6f, 0.2f, 1), true).getMaterial());
        }
    
        geom.setShadowMode(ShadowMode.CastAndReceive);

        geom.setUserData("oid", id.getId());    
        return geom;
    }


}

/*
    protected Spatial createPartSpatial( String id, Part part, ColorRGBA color, Map<Part, Spatial> childIndex ) {
        if( part instanceof CellArrayPart ) {
            return createPartSpatial(id, (CellArrayPart)part, color, true, childIndex);
        } else if( part instanceof Group ) {
            return createPartSpatial(id, (Group)part, color, childIndex);
        }
        return null;
    }

    protected Spatial createPartSpatial( String id, Group group, ColorRGBA color, Map<Part, Spatial> childIndex ) {
        Node node = new Node("Object:" + id);
        
        node.attachChild(createBox(0.1f, ColorRGBA.Red));
        
        // The root level will need to be positioned relative to the rigid body
        // which is positioned at CoG.
        Node cogOffset = new Node("CoG:" + id);
        node.attachChild(cogOffset);
        Vec3d cog = group.getMass().getCog();
log.info("moving root by -cog:" + cog);        
        cogOffset.move((float)-cog.x, (float)-cog.y, (float)-cog.z);
        
        createPartSpatial(cogOffset, id, group, color, childIndex);
        
        return node;
    }

    protected Spatial createPartSpatial( Node parent, String id, Group group, ColorRGBA color, Map<Part, Spatial> childIndex ) {
        for( Part child : group.getChildren() ) {            
            if( child instanceof CellArrayPart ) {
                Spatial ps = createPartSpatial(id, (CellArrayPart)child, color, false, childIndex);
                ps.setLocalTranslation(child.getShapeRelativePosition().toVector3f());
                ps.setLocalRotation(child.getShapeRelativeOrientation().toQuaternion());
                parent.attachChild(ps);                
            } else if( child instanceof Group ) {
                createPartSpatial(parent, id, (Group)child, color, childIndex);
            } 
        }
        return parent;
    }

    protected Spatial createPartSpatial( String id, CellArrayPart part, ColorRGBA color, boolean cogShift, Map<Part, Spatial> childIndex ) {
        if( part.getCells() == null ) {
            // Just a sphere
            float radius = (float)part.getMass().getRadius();  
            //Mesh mesh = new Box(radius, radius, radius);
            Sphere mesh = new Sphere(24, 24, radius);
            mesh.setTextureMode(Sphere.TextureMode.Projected);
            mesh.scaleTextureCoordinates(new Vector2f(4, 2));
            Geometry geom = new Geometry("Part:" + id, mesh);
            geom.setMaterial(GuiGlobals.getInstance().createMaterial(color, true).getMaterial());
            
            Texture texture = GuiGlobals.getInstance().loadTexture("Interface/grid-shaded-labeled.png", true, true);
            geom.getMaterial().setTexture("DiffuseMap", texture);
    
            geom.setShadowMode(ShadowMode.CastAndReceive);

            childIndex.put(part, geom);
                
            return geom;
        } else {
            Node node = new Node("Part:" + id);
            Node parts = new Node("cells:" + id);
            node.attachChild(parts);

            // If we set the local scale on the node then we can't add cool
            // debug stuff without unscaling.  Better to scale just the children, I think.                 
            //node.setLocalScale((float)object.cellScale);
            geomIndex.generateBlocks(parts, part.getCells()); //object.cells);                
 
            BodyMass bm = part.getMass();
                
            if( cogShift ) {
                // The position of the object is its CoG... which means
                // we need to offset our model's origin by it.  It should
                // already be scaled and everything... just need to negate it.
                Vector3f cogOffset = bm.getCog().toVector3f().negate(); 
                
                // We need to sort out what the center should be.  Directly out of generateBlocks()
                // the geometry is all relative to the corner.   See cog-offset.txt
                parts.move(cogOffset);
            }
            parts.setLocalScale((float)part.getScale()); //object.cellScale);   
            parts.setShadowMode(ShadowMode.CastAndReceive);

            childIndex.put(part, node);
                
            return node;
        }
    }


    protected Spatial createModel( EntityId id, MBlockShape shape, Mass mass ) {
 
        Spatial spatial;   
        CellArray cells = shape.getCells();        
        if( cells != null ) {
            Node node = new Node("Object:" + id);
            Node parts = new Node("Parts:" + id);
            node.attachChild(parts);
            spatial =  node;

            geomIndex.generateBlocks(parts, cells);                
 
            // For the time being, we'll just consider the CoG to be the center
            // of the model.
            //Vector3f cogOffset = cells.getSize().toVector3f();
            //cogOffset.multLocal((float)(-0.5 * shape.getScale()));
                
            BodyMass bm = shape.getMass();
                
            // The position of the object is its CoG... which means
            // we need to offset our model's origin by it.  It should
            // already be scaled and everything... just need to negate it.
            Vector3f cogOffset = bm.getCog().toVector3f().negate(); 
                
            // We need to sort out what the center should be.  Directly out of generateBlocks()
            // the geometry is all relative to the corner.   See cog-offset.txt
            parts.move(cogOffset);
            parts.setLocalScale((float)shape.getScale());   
            parts.setShadowMode(ShadowMode.CastAndReceive);

        } else {
            float radius = (float)shape.getMass().getRadius();  
            Sphere mesh = new Sphere(24, 24, radius);
            mesh.setTextureMode(Sphere.TextureMode.Projected);
            mesh.scaleTextureCoordinates(new Vector2f(4, 2));
            Geometry geom = new Geometry("Object:" + id, mesh);
            spatial = geom;
            
            if( mass != null && mass.getMass() != 0 ) { 
                geom.setMaterial(GuiGlobals.getInstance().createMaterial(new ColorRGBA(0, 0.6f, 0.6f, 1), true).getMaterial());
            
                Texture texture = GuiGlobals.getInstance().loadTexture("Interface/grid-shaded-labeled.png", true, true);
                geom.getMaterial().setTexture("DiffuseMap", texture);
            } else {
                // Just a flat green
                geom.setMaterial(GuiGlobals.getInstance().createMaterial(new ColorRGBA(0.2f, 0.6f, 0.2f, 1), true).getMaterial());
            }
    
            spatial.setShadowMode(ShadowMode.CastAndReceive);
        }
 
        spatial.setUserData("oid", id.getId());
        
        return spatial;        
    }
*/