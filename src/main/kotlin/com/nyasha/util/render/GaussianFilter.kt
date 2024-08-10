package com.nyasha.util.render

import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.Kernel
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sqrt

class GaussianFilter(private var radius: Float) {
    private var kernel: Kernel? = null

    fun filter(src: BufferedImage, dst: BufferedImage?): BufferedImage? {
        var dst = dst
        val width = src.width
        val height = src.height
        if (dst == null) {
            dst = createCompatibleDestImage(src, null)
        }
        val inPixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        src.getRGB(0, 0, width, height, inPixels, 0, width)
        if (this.radius > 0.0f) {
            convolveAndTranspose(kernel!!, inPixels, outPixels, width, height, true, true, false, 1)
            convolveAndTranspose(kernel!!, outPixels, inPixels, height, width, true, false, true, 1)
        }
        dst.setRGB(0, 0, width, height, inPixels, 0, width)
        return dst
    }

    fun createCompatibleDestImage(src: BufferedImage, dstCM: ColorModel?): BufferedImage {
        var dstCM = dstCM
        if (dstCM == null) {
            dstCM = src.colorModel
        }
        return BufferedImage(
            dstCM,
            dstCM!!.createCompatibleWritableRaster(src.width, src.height),
            dstCM.isAlphaPremultiplied,
            null
        )
    }

    override fun toString(): String {
        return "Blur/Gaussian Blur..."
    }

    companion object {
        fun convolveAndTranspose(
            kernel: Kernel,
            inPixels: IntArray,
            outPixels: IntArray,
            width: Int,
            height: Int,
            alpha: Boolean,
            premultiply: Boolean,
            unpremultiply: Boolean,
            edgeAction: Int
        ) {
            val matrix = kernel.getKernelData(null)
            val cols = kernel.width
            val cols2 = cols / 2
            for (y in 0 until height) {
                var index = y
                val ioffset = y * width
                for (x in 0 until width) {
                    var r = 0.0f
                    var g = 0.0f
                    var b = 0.0f
                    var a = 0.0f
                    val moffset = cols2
                    for (col in -cols2..cols2) {
                        val f = matrix[moffset + col]
                        if (f != 0.0f) {
                            var ix = x + col
                            if (ix < 0) {
                                if (edgeAction == 1) {
                                    ix = 0
                                } else if (edgeAction == 2) {
                                    ix = (x + width) % width
                                }
                            } else if (ix >= width) {
                                if (edgeAction == 1) {
                                    ix = width - 1
                                } else if (edgeAction == 2) {
                                    ix = (x + width) % width
                                }
                            }

                            val rgb = inPixels[ioffset + ix]
                            val pa = rgb shr 24 and 0xFF
                            var pr = rgb shr 16 and 0xFF
                            var pg = rgb shr 8 and 0xFF
                            var pb = rgb and 0xFF

                            if (premultiply) {
                                val a255 = pa * 0.003921569f
                                pr = (pr * a255).toInt()
                                pg = (pg * a255).toInt()
                                pb = (pb * a255).toInt()
                            }

                            a += f * pa
                            r += f * pr
                            g += f * pg
                            b += f * pb
                        }
                    }

                    if (unpremultiply && a != 0.0f && a != 255.0f) {
                        val f = 255.0f / a
                        r *= f
                        g *= f
                        b *= f
                    }

                    val ia = if (alpha) clamp((a + 0.5).toInt()) else 255
                    val ir = clamp((r + 0.5).toInt())
                    val ig = clamp((g + 0.5).toInt())
                    val ib = clamp((b + 0.5).toInt())
                    outPixels[index] = ia shl 24 or (ir shl 16) or (ig shl 8) or ib
                    index += height
                }
            }
        }

        fun clamp(c: Int): Int {
            if (c < 0) return 0
            return min(c.toDouble(), 255.0).toInt()
        }

        fun makeKernel(radius: Float): Kernel {
            val r = ceil(radius.toDouble()).toInt()
            val rows = r * 2 + 1
            val matrix = FloatArray(rows)
            val sigma = radius / 3.0f
            val sigma22 = 2.0f * sigma * sigma
            val sigmaPi2 = 6.2831855f * sigma
            val sqrtSigmaPi2 = sqrt(sigmaPi2.toDouble()).toFloat()
            val radius2 = radius * radius
            var total = 0.0f
            var index = 0
            for (row in -r..r) {
                val distance = (row * row).toFloat()
                if (distance > radius2) {
                    matrix[index] = 0.0f
                } else {
                    matrix[index] = exp((-distance / sigma22).toDouble()).toFloat() / sqrtSigmaPi2
                }
                total += matrix[index]
                index++
            }
            for (i in 0 until rows) {
                matrix[i] = matrix[i] / total
            }
            return Kernel(rows, 1, matrix)
        }
    }
}
