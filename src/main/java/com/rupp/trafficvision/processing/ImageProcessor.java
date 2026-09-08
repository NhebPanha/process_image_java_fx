package com.rupp.trafficvision.processing;

import com.rupp.trafficvision.model.ProcessingParameters;
import java.awt.image.BufferedImage;

/**
 * Strategy interface defining an image transformation or filtering algorithm.
 */
public interface ImageProcessor {

    /**
     * Applies this processor's algorithm to the supplied image.
     *
     * @param input  the source image
     * @param params active processing parameters and tunables
     * @return transformed new BufferedImage
     */
    BufferedImage process(BufferedImage input, ProcessingParameters params);
}
