package loyfael.litefish.utils;

import loyfael.litefish.LiteFish;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling messages and formatting
 */
public class MessageUtils {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");
    
    /**
     * Send a message to a player with color formatting
     */
    public static void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        
        String prefix = LiteFish.getInstance().getConfigManager()
                .getMessagesConfig().getString("prefix", "&b[LiteFish] &f");
        
        player.sendMessage(colorize(prefix + message));
    }
    
    /**
     * Send a message to a command sender with color formatting
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) return;
        
        String prefix = LiteFish.getInstance().getConfigManager()
                .getMessagesConfig().getString("prefix", "&b[LiteFish] &f");
        
        sender.sendMessage(colorize(prefix + message));
    }
    
    /**
     * Send a message without prefix
     */
    public static void sendRawMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) return;
        sender.sendMessage(colorize(message));
    }
    
    /**
     * Send a console message
     */
    public static void sendConsole(String message) {
        Bukkit.getConsoleSender().sendMessage(colorize(message));
    }
    
    /**
     * Broadcast a message to all players
     */
    public static void broadcast(String message) {
        Bukkit.broadcastMessage(colorize(message));
    }
    
    /**
     * Send an action bar message to a player
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) return;
        
        // Use spigot's sendActionBar method
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                                   net.md_5.bungee.api.chat.TextComponent.fromLegacyText(colorize(message)));
    }
    
    /**
     * Color a string with both legacy (&) and hex color codes
     */
    public static String colorize(String message) {
        if (message == null) return "";
        
        // Handle hex colors first
        message = translateHexColors(message);
        
        // Handle legacy colors
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Translate hex color codes from {#RRGGBB} to Minecraft format
     */
    private static String translateHexColors(String message) {
        if (!supportsHexColors()) {
            return message; // Remove hex colors if not supported
        }
        
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, "§x"
                    + "§" + group.charAt(0) + "§" + group.charAt(1)
                    + "§" + group.charAt(2) + "§" + group.charAt(3)
                    + "§" + group.charAt(4) + "§" + group.charAt(5));
        }
        
        return matcher.appendTail(buffer).toString();
    }
    
    /**
     * Check if the server supports hex colors (1.16+)
     */
    private static boolean supportsHexColors() {
        try {
            // Try to access a 1.16+ method
            Class.forName("net.md_5.bungee.api.ChatColor").getMethod("of", String.class);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return false;
        }
    }
    
    /**
     * Strip all color codes from a string
     */
    public static String stripColors(String message) {
        if (message == null) return "";
        
        // Remove hex colors
        message = HEX_PATTERN.matcher(message).replaceAll("");
        
        // Remove legacy colors
        return ChatColor.stripColor(message);
    }
    
    /**
     * Get a formatted message from config
     */
    public static String getMessage(String key) {
        return LiteFish.getInstance().getConfigManager()
                .getMessagesConfig().getString(key, "Missing message: " + key);
    }
    
    /**
     * Get a formatted message from config with replacements
     */
    public static String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        return message;
    }
    
    /**
     * Send a formatted message from config to a player
     */
    public static void sendConfigMessage(Player player, String key) {
        sendMessage(player, getMessage(key));
    }
    
    /**
     * Send a formatted message from config to a command sender
     */
    public static void sendConfigMessage(CommandSender sender, String key) {
        sendMessage(sender, getMessage(key));
    }
    
    /**
     * Send a formatted message from config with replacements
     */
    public static void sendConfigMessage(CommandSender sender, String key, String... replacements) {
        sendMessage(sender, getMessage(key, replacements));
    }
    
    /**
     * Format a number with commas
     */
    public static String formatNumber(double number) {
        return String.format("%,.2f", number);
    }
    
    /**
     * Format a number as integer with commas
     */
    public static String formatNumber(int number) {
        return String.format("%,d", number);
    }
    
    /**
     * Center a message for chat
     */
    public static String centerMessage(String message) {
        if (message == null || message.isEmpty()) return "";
        
        final int CENTER_PX = 154;
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;
        
        for (char c : message.toCharArray()) {
            if (c == '§') {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                isBold = (c == 'l' || c == 'L');
            } else {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }
        
        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        
        return sb.toString() + message;
    }
    
    /**
     * Enum for default font character information
     */
    private enum DefaultFontInfo {
        A('A', 5),
        a('a', 5),
        B('B', 5),
        b('b', 5),
        C('C', 5),
        c('c', 5),
        D('D', 5),
        d('d', 5),
        E('E', 5),
        e('e', 5),
        F('F', 5),
        f('f', 4),
        G('G', 5),
        g('g', 5),
        H('H', 5),
        h('h', 5),
        I('I', 3),
        i('i', 1),
        J('J', 5),
        j('j', 5),
        K('K', 5),
        k('k', 4),
        L('L', 5),
        l('l', 1),
        M('M', 5),
        m('m', 5),
        N('N', 5),
        n('n', 5),
        O('O', 5),
        o('o', 5),
        P('P', 5),
        p('p', 5),
        Q('Q', 5),
        q('q', 5),
        R('R', 5),
        r('r', 5),
        S('S', 5),
        s('s', 5),
        T('T', 5),
        t('t', 4),
        U('U', 5),
        u('u', 5),
        V('V', 5),
        v('v', 5),
        W('W', 5),
        w('w', 5),
        X('X', 5),
        x('x', 5),
        Y('Y', 5),
        y('y', 5),
        Z('Z', 5),
        z('z', 5),
        NUM_1('1', 5),
        NUM_2('2', 5),
        NUM_3('3', 5),
        NUM_4('4', 5),
        NUM_5('5', 5),
        NUM_6('6', 5),
        NUM_7('7', 5),
        NUM_8('8', 5),
        NUM_9('9', 5),
        NUM_0('0', 5),
        EXCLAMATION_POINT('!', 1),
        AT_SYMBOL('@', 6),
        NUM_SIGN('#', 5),
        DOLLAR_SIGN('$', 5),
        PERCENT('%', 5),
        UP_ARROW('^', 5),
        AMPERSAND('&', 5),
        ASTERISK('*', 5),
        LEFT_PARENTHESIS('(', 4),
        RIGHT_PERENTHESIS(')', 4),
        MINUS('-', 5),
        UNDERSCORE('_', 5),
        PLUS_SIGN('+', 5),
        EQUALS_SIGN('=', 5),
        LEFT_CURL_BRACE('{', 4),
        RIGHT_CURL_BRACE('}', 4),
        LEFT_BRACKET('[', 3),
        RIGHT_BRACKET(']', 3),
        COLON(':', 1),
        SEMI_COLON(';', 1),
        DOUBLE_QUOTE('"', 3),
        SINGLE_QUOTE('\'', 1),
        LEFT_ARROW('<', 4),
        RIGHT_ARROW('>', 4),
        QUESTION_MARK('?', 5),
        SLASH('/', 5),
        BACK_SLASH('\\', 5),
        LINE('|', 1),
        TILDE('~', 5),
        TICK('`', 2),
        PERIOD('.', 1),
        COMMA(',', 1),
        SPACE(' ', 3),
        DEFAULT('a', 4);
        
        private final char character;
        private final int length;
        
        DefaultFontInfo(char character, int length) {
            this.character = character;
            this.length = length;
        }
        
        public char getCharacter() {
            return this.character;
        }
        
        public int getLength() {
            return this.length;
        }
        
        public int getBoldLength() {
            if (this == DefaultFontInfo.SPACE) return this.getLength();
            return this.length + 1;
        }
        
        public static DefaultFontInfo getDefaultFontInfo(char c) {
            for (DefaultFontInfo dFI : DefaultFontInfo.values()) {
                if (dFI.getCharacter() == c) return dFI;
            }
            return DefaultFontInfo.DEFAULT;
        }
    }
}
