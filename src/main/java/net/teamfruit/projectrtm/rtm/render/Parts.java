package net.teamfruit.projectrtm.rtm.render;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.teamfruit.projectrtm.ngtlib.renderer.DisplayList;
import net.teamfruit.projectrtm.ngtlib.renderer.GLHelper;
import net.teamfruit.projectrtm.ngtlib.renderer.NGTRenderHelper;
import net.teamfruit.projectrtm.ngtlib.renderer.model.GroupObject;
import net.teamfruit.projectrtm.ngtlib.renderer.model.IModelNGT;
import net.teamfruit.projectrtm.ngtlib.util.NGTUtil;

@SideOnly(Side.CLIENT)
public class Parts {
	public final String[] objNames;
	private GroupObject[] objs;
	private DisplayList[] gLists;

	public Parts(String... par1) {
		this.objNames = par1;
	}

	public void init(PartsRenderer renderer) {
		this.gLists = new DisplayList[renderer.modelObj.textures.length];
	}

	public GroupObject[] getObjects(IModelNGT model) {
		if (this.objs==null) {
			this.objs = new GroupObject[this.objNames.length];
			for (int i = 0; i<this.objs.length; ++i) {
				for (GroupObject obj : model.getGroupObjects()) {
					if (this.objNames[i].equals(obj.name)) {
						this.objs[i] = obj;
						break;
					}
				}
			}
		}
		return this.objs;
	}

	public boolean containsName(String name) {
		return NGTUtil.contains(this.objNames, name);
	}

	public void render(PartsRenderer par1) {
		boolean smoothing = par1.modelSet.getConfig().smoothing;
		IModelNGT model = par1.modelObj.model;
		if (model.getGroupObjects().isEmpty())//NGTZ
		{
			model.renderOnly(smoothing, this.objNames);
		} else {
			int i = par1.currentMatId;
			if (!GLHelper.isValid(this.gLists[i])) {
				this.gLists[i] = GLHelper.generateGLList();
				GLHelper.startCompile(this.gLists[i]);
				NGTRenderHelper.renderCustomModel(model, (byte) i, smoothing, this.objNames);
				GLHelper.endCompile();
			} else {
				GLHelper.callList(this.gLists[i]);
			}
		}
	}
}