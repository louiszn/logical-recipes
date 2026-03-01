package xyz.louiszn.logicalrecipes.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;
import xyz.louiszn.logicalrecipes.LogicalRecipes;

public class LogicalRecipesCommand {
	private final LogicalRecipes plugin;

	public LogicalRecipesCommand(LogicalRecipes plugin) {
		this.plugin = plugin;
	}

	@Default
	@Command("logicalrecipes|lrc|lrecipes")
	@CommandDescription("Main plugin command")
	public void command(CommandSourceStack source) {
		source.getSender().sendMessage("Hello world – use /logicalrecipes help");
	}

	@Command("logicalrecipes reload")
	@CommandDescription("Reloads the plugin configuration")
	@Permission("logicalrecipes.reload")
	public void reload(CommandSourceStack source) {
		plugin.onReload();
		source.getSender().sendMessage("Configuration reloaded!");
	}

	@Command("logicalrecipes list")
	@CommandDescription("Lists all recipes")
	@Permission("logicalrecipes.list")
	public void list(CommandSourceStack source) {
		source.getSender().sendMessage("Listing recipes...");
	}
}
