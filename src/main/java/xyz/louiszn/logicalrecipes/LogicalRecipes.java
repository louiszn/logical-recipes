package xyz.louiszn.logicalrecipes;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.PaperCommandManager;
import xyz.louiszn.logicalrecipes.command.LogicalRecipesCommand;
import xyz.louiszn.logicalrecipes.config.ConfigManager;
import xyz.louiszn.logicalrecipes.listener.SmithingListener;

import java.util.List;

public final class LogicalRecipes extends JavaPlugin {
	public ConfigManager config;

	public LogicalRecipes() {
		this.config = new ConfigManager(this);
	}

	@Override
	public void onEnable() {
		config.load();
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
}
