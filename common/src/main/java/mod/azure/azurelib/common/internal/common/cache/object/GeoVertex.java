/**
 * This class is a fork of the matching class found in the Geckolib repository.
 * Original source: https://github.com/bernie-g/geckolib
 * Copyright © 2024 Bernie-G.
 * Licensed under the MIT License.
 * https://github.com/bernie-g/geckolib/blob/main/LICENSE
 */
package mod.azure.azurelib.common.internal.common.cache.object;

import org.joml.Vector3f;

/**
 * Vertex data holder
 *
 * @param position The position of the vertex
 * @param texU     The texture U coordinate
 * @param texV     The texture V coordinate
 */
public record GeoVertex(
        Vector3f position,
        float texU,
        float texV
) {

    public GeoVertex(double x, double y, double z) {
        this(new Vector3f((float) x, (float) y, (float) z), 0, 0);
    }

    public GeoVertex withUVs(float texU, float texV) {
        return new GeoVertex(this.position, texU, texV);
    }
}
