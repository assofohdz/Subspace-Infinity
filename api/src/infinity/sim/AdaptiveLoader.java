/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package infinity.sim;

import java.io.IOException;

import org.ini4j.Ini;

/**
 *
 * @author Asser Fahrenholz
 */
public interface AdaptiveLoader {

    boolean validateSettings(Ini settings) throws IOException;

}
