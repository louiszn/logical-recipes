package xyz.louiszn.logicalrecipes;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import xyz.louiszn.logicalrecipes.command.LogicalRecipesCommand;
import xyz.louiszn.logicalrecipes.config.ConfigManager;
import xyz.louiszn.logicalrecipes.config.RecipeConfig;
import xyz.louiszn.logicalrecipes.listener.SmithingListener;

import java.util.HashSet;
import java.util.Iterator;

public final class LogicalRecipes extends JavaPlugin {
	public ConfigManager config;

	public final HashSet<NamespacedKey> ghostRecipeKeys = new HashSet<>();

	public LogicalRecipes() {
		this.config = new ConfigManager(this);
	}

	@Override
	public void onEnable() {
		config.load();
		registerGhostRecipes();
		registerCommands();
		registerListeners();
		getLogger().info("Enabled LogicalRecipes");
	}

	@Override
	public void onDisable() {
		getLogger().info("Disabled LogicalRecipes");
	}

	public void onReload() {
		config.load();
		registerGhostRecipes();
		getLogger().info("Reloaded LogicalRecipes");
	}

	public void registerListeners() {
		Server server = getServer();
		PluginManager pluginManager = server.getPluginManager();

		pluginManager.registerEvents(new SmithingListener(this), this);
	}

	public void registerCommands() {
		PaperCommandManager<CommandSourceStack> commandManager = PaperCommandManager.builder()
				.executionCoordinator(ExecutionCoordinator.simpleCoordinator())
				.buildOnEnable(this);

		AnnotationParser<CommandSourceStack> annotationParser = new AnnotationParser<>(
				commandManager,
				CommandSourceStack.class
		);

		annotationParser.parse(new LogicalRecipesCommand(this));
	}

	public void registerGhostRecipes() {
		unregisterGhostRecipes();

		ItemStack ghostResult = new ItemStack(Material.STRUCTURE_VOID);
		ItemMeta meta = ghostResult.getItemMeta();
		meta.getPersistentDataContainer().set(
				new NamespacedKey(this, "ghost_recipe"),
				PersistentDataType.BYTE,
				(byte) 1
		);
		ghostResult.setItemMeta(meta);

		config.recipes.forEach((id, recipeObj) -> {
			if (!(recipeObj instanceof RecipeConfig.Smithing recipe)) return;

			Material templateMat = parseMaterial(recipe.ingredients.template.material());
			Material baseMat = parseMaterial(recipe.ingredients.base.material());
			Material additionMat = parseMaterial(recipe.ingredients.addition.material());

			if (templateMat == null || baseMat == null || additionMat == null) {
				getLogger().warning("Invalid materials in smithing recipe: " + id);
				return;
			}

			NamespacedKey key;

			// If override exists, remove vanilla and reuse its key
			if (recipe.override != null && !recipe.override.isEmpty()) {
				key = NamespacedKey.fromString(recipe.override);

				if (key == null) {
					getLogger().warning("Invalid override key in recipe: " + id);
					return;
				}

				Bukkit.removeRecipe(key);
				getLogger().info("Removed vanilla recipe: " + key);
			} else {
				key = new NamespacedKey(this, "ghost_" + id);
			}

			SmithingTransformRecipe ghost = new SmithingTransformRecipe(
					key,
					ghostResult,
					new RecipeChoice.MaterialChoice(templateMat),
					new RecipeChoice.MaterialChoice(baseMat),
					new RecipeChoice.MaterialChoice(additionMat)
			);

			Bukkit.addRecipe(ghost);
			ghostRecipeKeys.add(key);

			getLogger().info("Registered ghost recipe: " + key);
		});
	}

	public void unregisterGhostRecipes() {
		if (ghostRecipeKeys.isEmpty()) return;

		Iterator<Recipe> it = Bukkit.recipeIterator();

		while (it.hasNext()) {
			Recipe recipe = it.next();

			if (recipe instanceof Keyed keyed) {
				if (ghostRecipeKeys.contains(keyed.getKey())) {
					it.remove();
				}
			}
		}

		ghostRecipeKeys.clear();
	}

	private Material parseMaterial(String materialString) {
		if (materialString == null) return null;

		if (materialString.startsWith("minecraft:")) {
			materialString = materialString.substring("minecraft:".length());
		}

		return Material.getMaterial(materialString.toUpperCase());
	}
}
