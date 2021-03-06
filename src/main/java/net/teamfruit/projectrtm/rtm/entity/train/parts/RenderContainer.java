package net.teamfruit.projectrtm.rtm.entity.train.parts;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.teamfruit.projectrtm.rtm.RTMCore;
import net.teamfruit.projectrtm.rtm.modelpack.modelset.ModelSetContainerClient;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderContainer extends Render {
	private final void renderContainer(EntityContainer entity, double par2, double par4, double par6, float par8, float par9) {
		GL11.glPushMatrix();
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glTranslatef((float) par2, (float) par4, (float) par6);
		GL11.glRotatef(entity.rotationYaw, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-entity.rotationPitch, 1.0F, 0.0F, 0.0F);
		ModelSetContainerClient modelSet = (ModelSetContainerClient) entity.getModelSet();
		if (modelSet==null||modelSet.isDummy()) {
			RTMCore.proxy.renderMissingModel();
		} else {
			this.bindTexture(modelSet.texture);
			modelSet.model.renderAll(modelSet.getConfig().smoothing);
		}
		GL11.glPopMatrix();
	}

	@Override
	public void doRender(Entity par1, double par2, double par4, double par6, float par8, float par9) {
		this.renderContainer((EntityContainer) par1, par2, par4, par6, par8, par9);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity par1) {
		return null;
	}

	@Override
	protected void bindEntityTexture(Entity entiy) {
	}
}