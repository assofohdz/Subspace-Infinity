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

package infinity.es.input;

import com.simsilica.mathd.*;
import com.simsilica.es.EntityComponent;

/**
 *  Represents the abstract movement inputs from a NPC.  The
 *  control driver will translate this into physical movement.
 *
 *  @author    Paul Speed
 */
public class CharacterInput implements EntityComponent {

    public static final byte NONE = 0x0;
    public static final byte JUMP = 0x1;

    private Vec3d move; 
    private Quatd facing;
    private byte flags;
    
    private CharacterInput() {
    }
    
    public CharacterInput( Vec3d move, Quatd facing, byte flags ) {
        this.move = move;
        this.facing = facing;
        this.flags = flags;
    }
    
    public Vec3d getMove() {
        return move;
    }
    
    public Quatd getFacing() {
        return facing;
    }
    
    public byte getFlags() {
        return flags;
    }
    
    public boolean isJumping() {
        return (flags & JUMP) != 0;
    }
    
    public String toString() {
        return "CharacterInput[move=" + move + ", facing=" + facing + ", flags=" + Integer.toHexString(flags) + "]";
    } 
}
