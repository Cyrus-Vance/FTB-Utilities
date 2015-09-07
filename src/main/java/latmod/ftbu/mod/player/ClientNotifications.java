package latmod.ftbu.mod.player;

import java.io.File;
import java.net.URI;

import latmod.ftbu.core.Notification;
import latmod.ftbu.core.client.LatCoreMCClient;
import latmod.ftbu.core.gui.GuiLM;
import latmod.ftbu.core.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.event.ClickEvent;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ClientCommandHandler;

import org.lwjgl.opengl.*;

import cpw.mods.fml.relauncher.*;

@SideOnly(Side.CLIENT)
public class ClientNotifications
{
	private static Temp current = null;
	
	public static void renderTemp(Minecraft mc)
	{
		if(current != null)
		{
			current.render(mc);
			if(current.isDead())
				current = null;
		}
		else if(!Temp.list.isEmpty())
		{
			current = Temp.list.get(0);
			Temp.list.remove(0);
		}
	}
	
	public static void add(Notification n)
	{
		if(n == null) return;
		if(n.ID != null)
		{
			Temp.list.removeObj(n.ID);
			Perm.list.removeObj(n.ID);
			if(current != null && current.notification.ID != null && current.notification.ID.equals(n.ID))
				current = null;
		}
		
		Temp.list.add(new Temp(n));
		if(!n.isTemp()) Perm.list.add(new Perm(n));
	}
	
	public static void init()
	{
		current = null;
		Perm.list.clear();
		Temp.list.clear();
	}
	
	public static class Temp extends Gui
	{
		public static final FastList<Temp> list = new FastList<Temp>();
		
		public final Notification notification;
		
		private RenderItem renderItem = new RenderItem();
		private long time;

		public Temp(Notification n)
		{
			notification = n;
			time = -1L;
		}
		
		public boolean equals(Object o)
		{ return notification.equals(o); }
		
		public void render(Minecraft mc)
		{
			if(time == -1L) time = Minecraft.getSystemTime();
			
			if (time > 0L)
			{
				GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
				GL11.glMatrixMode(GL11.GL_PROJECTION);
				GL11.glLoadIdentity();
				GL11.glMatrixMode(GL11.GL_MODELVIEW);
				GL11.glLoadIdentity();
				GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
				GL11.glMatrixMode(GL11.GL_PROJECTION);
				GL11.glLoadIdentity();
				GL11.glOrtho(0D, LatCoreMCClient.displayW, LatCoreMCClient.displayH, 0D, 1000D, 3000D);
				GL11.glMatrixMode(GL11.GL_MODELVIEW);
				GL11.glLoadIdentity();
				GL11.glTranslatef(0F, 0F, -2000F);
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				GL11.glDepthMask(false);
				
				double d0 = (double)(Minecraft.getSystemTime() - time) / (double)notification.timer;
				
				if (d0 < 0D || d0 > 1D) { time = 0L; return; }
				
				double d1 = d0 * 2D;
				
				if (d1 > 1D) d1 = 2D - d1;
				d1 *= 4D;
				d1 = 1D - d1;

				if (d1 < 0D) d1 = 0D;
				
				d1 *= d1;
				d1 *= d1;
				
				String title = notification.title.getFormattedText();
				String desc = (notification.getDesc() == null) ? null : notification.getDesc().getFormattedText();
				ItemStack is = notification.getItem();
				
				int width = 20 + Math.max(mc.fontRenderer.getStringWidth(title), mc.fontRenderer.getStringWidth(desc));
				if(is != null) width += 20;
				
				int i = LatCoreMCClient.displayW - width;
				int j = 0 - (int)(d1 * 36D);
				GL11.glColor4f(1F, 1F, 1F, 1F);
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				GL11.glDisable(GL11.GL_LIGHTING);
				GuiLM.drawRect(i, j, LatCoreMCClient.displayW, j + 32, LMColorUtils.getRGBA(notification.getColor(), 140));
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				
				int w = is == null ? 10 : 30;
				
				if(desc == null)
				{
					mc.fontRenderer.drawString(title, i + w, j + 12, -256);
				}
				else
				{
					mc.fontRenderer.drawString(title, i + w, j + 7, -256);
					mc.fontRenderer.drawString(desc, i + w, j + 18, -1);
				}
				
				if(is == null) return;
				
				RenderHelper.enableGUIStandardItemLighting();
				GL11.glEnable(GL12.GL_RESCALE_NORMAL);
				GL11.glEnable(GL11.GL_COLOR_MATERIAL);
				GL11.glEnable(GL11.GL_LIGHTING);
				renderItem.renderItemIntoGUI(mc.fontRenderer, mc.getTextureManager(), is, i + 8, j + 8, false);
				renderItem.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), is, i + 8, j + 8);
				//renderItem.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), notification.item, i + 8, j + 8);
				GL11.glDisable(GL11.GL_LIGHTING);
				GL11.glDepthMask(true);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
			}
		}

		public boolean isDead()
		{ return time == 0L; }
	}
	
	public static class Perm implements Comparable<Perm>
	{
		public static final FastList<Perm> list = new FastList<Perm>();
		
		public final Notification notification;
		public final long timeAdded;
		
		public Perm(Notification n)
		{
			notification = n;
			timeAdded = LMUtils.millis();
		}
		
		public boolean equals(Object o)
		{ return notification.equals(o); }
		
		public int compareTo(Perm o)
		{ return Long.compare(timeAdded, o.timeAdded); }
		
		public void onClicked(Minecraft mc)
		{
			ClickEvent e = notification.getClickEvent();
			if(e != null && e.getAction() != null)
			{
				ClickEvent.Action a = e.getAction();
				String v = e.getValue();
				
				if(a == ClickEvent.Action.OPEN_URL)
				{
					try { LMUtils.openURI(new URI(v)); }
					catch (Exception ex) { ex.printStackTrace(); }
				}
				else if(a == ClickEvent.Action.OPEN_FILE)
				{
					try { LMUtils.openURI(new File(v).toURI()); }
					catch (Exception ex) { ex.printStackTrace(); }
				}
				else if(a == ClickEvent.Action.RUN_COMMAND)
				{
					mc.ingameGUI.getChatGUI().addToSentMessages(v);
			        if(ClientCommandHandler.instance.executeCommand(mc.thePlayer, v) != 0) return;
			        mc.thePlayer.sendChatMessage(v);
				}
				else if(a == ClickEvent.Action.SUGGEST_COMMAND)
				{
					mc.displayGuiScreen(new GuiChat(v));
				}
			}
		}
	}
}