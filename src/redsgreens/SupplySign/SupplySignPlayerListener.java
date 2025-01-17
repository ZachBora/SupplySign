package redsgreens.SupplySign;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;

/**
 * Handle events for all Player related events
 * @author redsgreens
 */
public class SupplySignPlayerListener extends PlayerListener {

	private final SupplySign Plugin;
	
    public SupplySignPlayerListener(SupplySign plugin) 
    {
    	Plugin = plugin;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event)
    // catch player right-click events
    {
    	// return if the event is already cancelled, or if it's not a right-click event
		if(event.isCancelled() || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		Block block = event.getClickedBlock();

		if (block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN_POST && block.getType() != Material.CHEST && block.getType() != Material.DISPENSER)
			return;
		
		Player player = event.getPlayer();
		
		Sign sign;
		if(block.getType() == Material.CHEST){
			sign = SupplySignUtil.getAttachedSign(block);
			if(sign == null) return;
		}
		else if(block.getType() == Material.DISPENSER){
			sign = SupplySignUtil.getAttachedSign(block);
			if(sign == null) return;
			else
			{ // prevent opening inventory of a dispenser with a supplysign attached
				event.setCancelled(true);
				if(Plugin.Config.ShowErrorsInClient)
					player.sendMessage("�cErr: SupplySign attached to dispenser, inventory unavailable.");
				return;
			}
		}
		else 
			sign = new CraftSign(block);
		
		try
		{
			if (SupplySignUtil.isSupplySign(sign)){
				event.setCancelled(true);

				if(sign.getBlock().getType() == Material.WALL_SIGN) // special checks for wall signs on chests or dispensers
				{
					Block blockBehindSign = SupplySignUtil.getBlockBehindWallSign(sign); 
					if(blockBehindSign.getType() == Material.DISPENSER) // if it's a dispenser cancel right click on sign

					{
						if(Plugin.Config.ShowErrorsInClient)
							player.sendMessage("�cErr: SupplySign attached to dispenser, inventory unavailable.");
						return;
					}
					else if(blockBehindSign.getType() == Material.CHEST && block.getType() == Material.WALL_SIGN) // if it's a chest simulate a click on the chest and return
					{
							Event e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getItemInHand(), blockBehindSign, event.getBlockFace());
							Plugin.getServer().getPluginManager().callEvent(e);
							event.setCancelled(true);
							return;
					}
				}

				ArrayList<Object> itemList = new ArrayList<Object>();
				
				// if it's a kit, test for generic access permission or access to this specific kit
				if(sign.getLine(1).trim().contains("kit:")){
					String[] split = sign.getLine(1).trim().split(":");
					
					if(Plugin.isAuthorized(player, "access") || Plugin.isAuthorized(player, "access." + split[1]))
						itemList = Plugin.Kits.getKit(split[1]);
					else if(Plugin.Config.ShowErrorsInClient)
						player.sendMessage("�cErr: you don't have permission to access this SupplySign.");
				}
				else
				{
					if(Plugin.isAuthorized(player, "access"))
					{
						// it's not a kit, so load the items from the lines on the sign
						String line1 = SupplySignUtil.stripColorCodes(sign.getLine(1).trim()); 
						String line2 = SupplySignUtil.stripColorCodes(sign.getLine(2).trim()); 
						String line3 = SupplySignUtil.stripColorCodes(sign.getLine(3).trim()); 
						if(!line1.equalsIgnoreCase(""))
							itemList.add(line1);
						if(!line2.equalsIgnoreCase(""))
							itemList.add(line2);
						if(!line3.equalsIgnoreCase(""))
							itemList.add(line3);
					}
					else if(Plugin.Config.ShowErrorsInClient)
						player.sendMessage("�cErr: you don't have permission to access this SupplySign.");
				}
				
				if(itemList.size() > 0)
					Plugin.Items.showInventory(player, itemList);
				
				return;
			}
		}
		catch (Throwable ex)
		{
			if(Plugin.Config.ShowErrorsInClient)
				player.sendMessage("�cErr: " + ex.getMessage());
		}
    }
}

