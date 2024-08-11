/*
 *     MCEF (Minecraft Chromium Embedded Framework)
 *     Copyright (C) 2023 CinemaMod Group
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */

package com.nyasha.mcef.internal;

import com.nyasha.managers.FontManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

public class MCEFDownloaderMenu extends Screen {
    private final TitleScreen menu;

    public MCEFDownloaderMenu(TitleScreen menu) {
        super(Text.literal("MCEF is downloading required libraries..."));
        this.menu = menu;
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        double cx = width / 2d;
        double cy = height / 2d;

        double progressBarHeight = 14;
        double progressBarWidth = width / 3d; // TODO: base off screen with (1/3 of screen)

        MatrixStack poseStack = context.getMatrices();

        /* Draw Progress Bar */
        poseStack.push();
        poseStack.translate(cx, cy, 0);
        poseStack.translate(-progressBarWidth / 2d, -progressBarHeight / 2d, 0);
        context.fill( // bar border
                0, 0,
                (int) progressBarWidth,
                (int) progressBarHeight,
                -1
        );
        context.fill( // bar padding
                2, 2,
                (int) progressBarWidth - 2,
                (int) progressBarHeight - 2,
                -16777215
        );
        context.fill( // bar bar
                4, 4,
                (int) ((progressBarWidth - 4) * MCEFDownloadListener.INSTANCE.getProgress()),
                (int) progressBarHeight - 4,
                -1
        );
        poseStack.pop();

        // putting this here incase I want to re-add a third line later on
        // allows me to generalize the code to not care about line count
        String[] text = new String[]{
                MCEFDownloadListener.INSTANCE.getTask(),
                Math.round(MCEFDownloadListener.INSTANCE.getProgress() * 100) + "%",
        };

        /* Draw Text */
        // calculate offset for the top line
        poseStack.push();

        // draw menu name
        FontManager.INSTANCE.getConther16().drawCenteredString(context.getMatrices(),title.getString(),width/2,5F+20,new Color(255,255,255));

        // draw text
        int index = 1;
        //FontManager.INSTANCE.getConther16().drawCenteredString(context.getMatrices(),title.getString(),width,5F,new Color(255,255,255));


        for (String s : text) {
            FontManager.INSTANCE.getConther16().drawCenteredString(context.getMatrices(),s,width/2,5F+18*index+20,new Color(255,255,255));
            index++;
        }
        poseStack.pop();

        // TODO: if listener.isFailed(), draw some "Failed to initialize MCEF" text with an "OK" button to proceed
    }

    @Override
    public void tick() {
        if (MCEFDownloadListener.INSTANCE.isDone() || MCEFDownloadListener.INSTANCE.isFailed()) {
            this.close();
            MinecraftClient.getInstance().setScreen(menu);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

}
