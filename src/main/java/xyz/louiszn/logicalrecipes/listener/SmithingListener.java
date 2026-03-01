package xyz.louiszn.logicalrecipes.listener;

import net.minecraft.world.item.crafting.SmithingRecipeInput;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import xyz.louiszn.logicalrecipes.LogicalRecipes;
import xyz.louiszn.logicalrecipes.config.RecipeConfig.Smithing;
import xyz.louiszn.logicalrecipes.config.RecipeConfig.Ingredient;
import xyz.louiszn.logicalrecipes.config.RecipeConfig.Result;

import java.util.Map;

public class SmithingListener implements Listener {
	private final LogicalRecipes plugin;
	private final NamespacedKey recipeKey;

	public SmithingListener(LogicalRecipes plugin) {
		this.plugin = plugin;
		this.recipeKey = new NamespacedKey(plugin, "custom_recipe_id");
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPrepareSmithing(PrepareSmithingEvent event) {
		SmithingInventory inv = event.getInventory();
		ItemStack template = inv.getItem(0);
		ItemStack base = inv.getItem(1);
		ItemStack addition = inv.getItem(2);

		if (template == null || base == null || addition == null) {
			return;
		}

		for (Map.Entry<String, ?> entry : plugin.config.recipes.entrySet()) {
			if (!(entry.getValue() instanceof Smithing recipe)) continue;

			if (!matchesIngredient(recipe.ingredients.template, template)) continue;
			if (!matchesIngredient(recipe.ingredients.base, base)) continue;
			if (!matchesIngredient(recipe.ingredients.addition, addition)) continue;

			if (template.getAmount() < recipe.ingredients.template.amount()) continue;
			if (base.getAmount() < recipe.ingredients.base.amount()) continue;
			if (addition.getAmount() < recipe.ingredients.addition.amount()) continue;

			Result resultData = recipe.result;
			Material resultMaterial = parseMaterial(resultData.material());
			if (resultMaterial == null) {
				return;
			}

			ItemStack result = new ItemStack(resultMaterial, resultData.amount());

			ItemMeta meta = result.getItemMeta();
			meta.getPersistentDataContainer().set(recipeKey, PersistentDataType.STRING, recipe.id);
			result.setItemMeta(meta);

			event.setResult(result);
			return;
		}

		for (String overrideKey : plugin.config.overriddenVanillaRecipes) {
			NamespacedKey key = NamespacedKey.fromString(overrideKey);

			if (key == null) continue;

			org.bukkit.inventory.Recipe vanillaRecipe = Bukkit.getRecipe(key);

			if (vanillaRecipe == null) continue;

			if (matchesVanillaRecipe(vanillaRecipe, template, base, addition)) {
				event.setResult(null);
				plugin.getLogger().fine("Blocked overridden vanilla recipe: " + overrideKey);
				break;
			}
		}
	}

	@EventHandler
	public void onSmithItem(SmithItemEvent event) {
		if (event.isCancelled()) return;

		ItemStack result = event.getCurrentItem();
		if (result == null || !result.hasItemMeta()) return;

		PersistentDataContainer pdc = result.getItemMeta().getPersistentDataContainer();
		String recipeId = pdc.get(recipeKey, PersistentDataType.STRING);
		if (recipeId == null) return;

		Object recipeObj = plugin.config.recipes.get(recipeId);
		if (!(recipeObj instanceof Smithing recipe)) {
			return;
		}

		event.setCancelled(true);

		SmithingInventory inv = event.getInventory();
		ItemStack template = inv.getItem(0);
		ItemStack base = inv.getItem(1);
		ItemStack addition = inv.getItem(2);

		if (template == null || base == null || addition == null) {
			return;
		}

		int templateConsume = recipe.ingredients.template.consume();
		int baseConsume = recipe.ingredients.base.consume();
		int additionConsume = recipe.ingredients.addition.consume();

		int finalTemplateAmount = template.getAmount() - (templateConsume);
		int finalBaseAmount = base.getAmount() - (baseConsume);
		int finalAdditionAmount = addition.getAmount() - (additionConsume);

		ItemStack item = result.clone();

		// Remove existing meta key
		ItemMeta meta = result.getItemMeta();
		meta.getPersistentDataContainer().remove(recipeKey);
		item.setItemMeta(meta);

		template.setAmount(finalTemplateAmount);
		base.setAmount(finalBaseAmount);
		addition.setAmount(finalAdditionAmount);
		result.setAmount(0);

		Player player = (Player) event.getView().getPlayer();

		// If the cursor is empty, we can set it, otherwise add to inventory
		if (player.getItemOnCursor().isEmpty() && !event.isShiftClick()) {
			player.setItemOnCursor(item);
		} else {
			player.getInventory().addItem(item).values().forEach(i ->
					player.getWorld().dropItem(player.getLocation(), i)
			);
		}

		player.playSound(
				player.getLocation(),
				Sound.BLOCK_SMITHING_TABLE_USE,
				1.0f,
				1.0f
		);
	}

	private boolean matchesIngredient(Ingredient ingredient, ItemStack stack) {
		Material expected = parseMaterial(ingredient.material());
		return expected != null && stack.getType() == expected;
	}

	private boolean matchesVanillaRecipe(Recipe recipe, ItemStack template, ItemStack base, ItemStack addition) {
		// Handle SmithingTransformRecipe (has template, base, addition)
		if (recipe instanceof SmithingTransformRecipe transform) {
			boolean templateMatches = transform.getTemplate().test(template);
			boolean baseMatches = transform.getBase().test(base);
			boolean additionMatches = transform.getAddition().test(addition);

			return templateMatches && baseMatches && additionMatches;
		}

		// Handle SmithingTrimRecipe (has template, base, addition, but no result item)
		if (recipe instanceof SmithingTrimRecipe trim) {
			boolean templateMatches = trim.getTemplate().test(template);
			boolean baseMatches = trim.getBase().test(base);
			boolean additionMatches = trim.getAddition().test(addition);

			return templateMatches && baseMatches && additionMatches;
		}

		// Fallback to old smithing with no templates
		if (recipe instanceof SmithingRecipe old) {
			plugin.getLogger().warning("Encountered legacy SmithingRecipe; cannot match template accurately.");

			boolean baseMatches = old.getBase().test(base);
			boolean additionMatches = old.getAddition().test(addition);

			return baseMatches && additionMatches;
		}

		return false;
	}

	private Material parseMaterial(String materialString) {
		if (materialString == null) return null;

		if (materialString.startsWith("minecraft:")) {
			materialString = materialString.substring("minecraft:".length());
		}

		return Material.getMaterial(materialString.toUpperCase());
	}
}
