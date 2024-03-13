import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.authme.api.AuthMeApi;
import com.authme.events.LoginEvent;
import com.authme.events.RegisterEvent;
import com.authme.events.UnregisterEvent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class AuthMeIntegration extends JavaPlugin implements Listener, CommandExecutor {

    private AuthMeApi authMeApi;
    private Map<Player, String> pendingRegistrations = new HashMap<>();

    @Override
    public void onEnable() {
        // Проверяем, установлен ли плагин AuthMe Reloaded
        if (getServer().getPluginManager().getPlugin("AuthMe") == null) {
            getLogger().warning("AuthMe Reloaded is not installed! This plugin requires AuthMe Reloaded to function properly.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Получаем экземпляр AuthMeApi
        authMeApi = AuthMeApi.getInstance();

        // Регистрируем обработчик событий и команды
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("login").setExecutor(this);
        getCommand("register").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("login")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /login <password>");
                return true;
            }

            String password = args[0];

            if (!authMeApi.isAuthenticated(player)) {
                // Проверяем правильность пароля
                if (authMeApi.checkPassword(player.getName(), password)) {
                    player.sendMessage("§aYou have successfully logged in!");
                } else {
                    player.sendMessage("§cInvalid password! Please try again.");
                }
            } else {
                player.sendMessage("§aYou are already logged in!");
            }

        } else if (command.getName().equalsIgnoreCase("register")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /register <password>");
                return true;
            }

            String password = args[0];

            if (authMeApi.isRegistered(player.getName())) {
                player.sendMessage("§cYou are already registered!");
                return true;
            }

            // Зашифровываем пароль перед сохранением
            String encryptedPassword = encryptPassword(password);
            pendingRegistrations.put(player, encryptedPassword);
            player.sendMessage("§aPlease confirm your registration by typing /confirmregister <password>.");

        } else if (command.getName().equalsIgnoreCase("confirmregister")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /confirmregister <password>");
                return true;
            }

            String password = args[0];

            // Проверяем, есть ли игрок в списке ожидающих регистрацию
            if (!pendingRegistrations.containsKey(player)) {
                player.sendMessage("§cYou are not in the registration process!");
                return true;
            }

            String encryptedPassword = pendingRegistrations.get(player);

            // Проверяем совпадение паролей
            if (!encryptPassword(password).equals(encryptedPassword)) {
                player.sendMessage("§cPasswords do not match! Please try again.");
                return true;
            }

            // Регистрируем игрока
            authMeApi.forceRegisterPlayer(player.getName(), encryptedPassword);
            player.sendMessage("§aYou have successfully registered!");

            // Удаляем игрока из списка ожидающих регистрацию
            pendingRegistrations.remove(player);
        }

        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!authMeApi.isRegistered(player.getName())) {
            player.sendMessage("§cYou need to register to play on this server! Use /register <password>.");
        } else if (!authMeApi.isAuthenticated(player)) {
            player.sendMessage("§cYou need to login to continue playing! Use /login <password>.");
        } else {
            // Если игрок зарегистрирован и авторизован, выполняем дальнейшие действия
            // Например, загрузка инвентаря или местоположения игрока
        }
    }

    @EventHandler
    public void onPlayerRegister(RegisterEvent event) {
        // Сюда можно добавить дополнительные действия при регистрации игрока
    }

    @EventHandler
    public void onPlayerLogin(LoginEvent event) {
        // Сюда можно добавить дополнительные действия при входе авторизованного игрока
    }

    @EventHandler
    public void onPlayerUnregister(UnregisterEvent event) {
        // Сюда можно добавить дополнительные действия при удалении регистрации игрока
    }

    private String encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            getLogger().warning("Failed to encrypt password: " + e.getMessage());
            return null;
        }
    }
}
