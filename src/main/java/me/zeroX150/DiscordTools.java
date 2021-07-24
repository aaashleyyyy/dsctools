package me.zeroX150;

public class DiscordTools {
    public static void main(String[] args) throws Exception {
        System.out.println("Important for this to work: Your bot has to have the \"members\" intent enabled");
        System.out.println("What's your **BOTS** token?");
        System.out.print("> ");
        String t = System.console().readLine();
        System.out.println("What guild do you want to rename everyone in? (Guild ID)");
        System.out.print("> ");
        String g = System.console().readLine();
        System.out.println("What do you want to rename everyone to?");
        System.out.print("> ");
        String m = System.console().readLine();
        new Runner().start(t, g, m);
    }

}
