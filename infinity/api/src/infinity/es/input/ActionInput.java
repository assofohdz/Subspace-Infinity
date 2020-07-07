/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package infinity.es.input;

import com.simsilica.es.EntityComponent;

/**
 *
 * @author AFahrenholz
 */
public class ActionInput implements EntityComponent {

    public static final byte PLACEBRICK = 0x0;
    public static final byte FIREBURST = 0x1;
    public static final byte PLACEDECOY = 0x2;
    public static final byte PLACEPORTAL = 0x3;
    public static final byte REPEL = 0x4;
    public static final byte FIREROCKET = 0x5;
    public static final byte FIRETHOR = 0x6;
    public static final byte WARP = 0x7;

    private byte flags;
    
    private ActionInput(){
    }

    public ActionInput(byte flags) {
        this.flags = flags;
    }

    public byte getFlags() {
        return flags;
    }

}
