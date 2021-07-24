package me.zeroX150;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;

import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Runner {
    List<String> logs = new ArrayList<>();
    long startTime = System.currentTimeMillis();
    int fps = 20;
    int renamed = 0;
    int skipped = 0;
    int failed = 0;
    int total = 0;
    Thread updater;
    volatile boolean exit = false;
    DateFormat df = new SimpleDateFormat("m:s");

    public void start(String token, String id, String name) throws Exception {
        updater = new Thread(this::update);
        updater.start();
        logs.add("Trying to login...");
        JDA jda;
        try {
            jda = JDABuilder.create(token, Collections.singletonList(GatewayIntent.GUILD_MEMBERS)).setChunkingFilter(ChunkingFilter.ALL).build();
        } catch (Exception e) {
            logs.add("Failed to login!");
            sleep(1000);
            stop();
            e.printStackTrace();
            System.out.println("Please check if you have an internet connection and your token was valid");
            return;
        }
        logs.add("Waiting for login...");
        jda.awaitReady();
        logs.add("Scanning guilds...");

        for (Guild guild : jda.getGuilds()) {
            logs.add("Found guild " + guild.getName() + " (" + guild.getId() + ")");
        }
        logs.add("Checking permissions...");
        Guild target = jda.getGuildById(id);
        if (target == null) {
            jda.shutdown();
            stop();
            System.out.println("Could not find target guild.");
            return;
        }
        if (!target.getSelfMember().hasPermission(Permission.NICKNAME_MANAGE)) {
            jda.shutdown();
            stop();
            System.out.println("The bot does not have the permission needed to change nicknames.");
            return;
        }
        logs.add("Starting in 3 seconds.");
        sleep(3000);
        logs.add("Loading members...");
        // gotta do this on a sep thread
        new Thread(() -> {
            AtomicBoolean b = new AtomicBoolean();
            List<Member> members = new ArrayList<>();
            target.loadMembers(member -> {
                if (!b.get()) b.set(true);
                total++;
                members.add(member);
            });
            while (!b.get()) {
                sleep(1);
            }
            List<Member> renamed = new ArrayList<>();
            while (members.stream().anyMatch(member -> !renamed.contains(member))) {
                Member member = members.stream().filter(member1 -> !renamed.contains(member1)).collect(Collectors.toList()).get(0);
                renamed.add(member);
                String v = Normalizer.normalize(member.getUser().getAsTag(), Normalizer.Form.NFD).replaceAll("\\P{InBasic_Latin}", "?");
                if (member.getNickname() != null && member.getNickname().equals(name)) {
                    logs.add("Skipping " + v);
                    skipped++;
                } else {
                    logs.add(v + " -> " + name);
                    try {
                        member.modifyNickname(name).complete();
                    } catch (Exception ignored) {
                        logs.add("Failed to rename " + member.getUser().getAsTag());
                    }
                }
                this.renamed++;
            }
            try {
                logs.add("Done renaming!");
                sleep(1000);
                logs.add("Logging out...");
                stop();
                jda.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    void stop() throws InterruptedException {
        exit = true;
        updater.join();
        System.out.println();
    }

    void update() {
        while (!exit) {
            sleep((long) (1000d / fps));
            List<String> col1 = new ArrayList<>();
            col1.add("Logs");
            col1.add("");
            List<String> logs = new ArrayList<>(Arrays.asList(this.logs.toArray(new String[0])));
            while (logs.size() > 40) logs.remove(0);
            col1.addAll(logs);

            String l =
                    "      _ _   \n" +
                            "     | | |  \n" +
                            "   __| | |_ \n" +
                            "  / _` | __|\n" +
                            " | (_| | |_ \n" +
                            "  \\__,_|\\__|";

            List<String> col2 = new ArrayList<>(Arrays.asList(l.split("\n")));
            col2.add("Running for " + df.format(System.currentTimeMillis() - startTime));
            col2.add("Progress: " + renamed + " / " + total);
            col2.add("Skipped: " + skipped);
            col2.add("Failed: " + failed);
            col2.add("Progress %: " + (total == 0 ? "0%" : ((int) Math.floor((double) renamed / total * 100d)) + "%"));

            int maxWidth = col1.stream().sorted(Comparator.comparingInt(value -> -value.length())).collect(Collectors.toList()).get(0).length();
            List<String> frame = new ArrayList<>();
            for (int i = 0; i < (col1.size() > col2.size() ? col1 : col2).size(); i++) {
                String e1 = col1.size() > i ? col1.get(i) : "";
                String e2 = col2.size() > i ? col2.get(i) : "";
                StringBuilder margin = new StringBuilder();
                for (int ii = 0; ii < Math.abs(maxWidth - e1.length()); ii++) margin.append(" ");
                frame.add(e1 + margin + "  |  " + e2);
            }
            clear();
            for (String s : frame) {
                System.out.println(s);
            }
        }
    }

    void clear() {
        System.out.print("\033c");
    }

    void sleep(long m) {
        try {
            Thread.sleep(m);
        } catch (Exception ignored) {
        }
    }
}
