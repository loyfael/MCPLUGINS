package loyfael.utils;

import loyfael.models.MobRarity;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Génère des noms absurdes, familles thématiques et punchlines pour les mobs custom.
 */
public final class MobFlavor {

    private static final Map<EntityType, MobTheme> THEME_BY_ENTITY = new EnumMap<>(EntityType.class);
    private static final Map<MobTheme, WordBank> WORD_BANKS = new EnumMap<>(MobTheme.class);
    private static final List<String> ELITE_NAMES = List.of(
        "Gloupiton des Marais Oubliés",
        "Fumignasse du Crépuscule",
        "Capitaine Ronchonouille",
        "Seigneur Couinard du Désert Humide",
        "Poupougnax le Trop-Mûr",
        "Archi-Puant des Sables",
        "Archiflaqueur du Fromage Bleu",
        "Grand Vizir Tripesournois",
        "Tordebide Premier du Nom",
        "Archi-Flaqueur d'Âmes"
    );
    private static final List<String> DEATH_QUIPS = List.of(
        "Il s'évapore dans un petit nuage de poussière et d'égo blessé.",
        "On retrouve son inventaire soigneusement empilé au sol.",
        "Les villageois voisins ferment leurs volets par précaution.",
        "Son dernier soupir ressemble à un soupçon de soulagement.",
        "Une odeur de torche humide flotte encore dans l'air.",
        "Ses bottes se coincent dans le sol, histoire de signaler l'endroit.",
        "Sa bannière tombe mollement et se couvre déjà de poussière.",
        "Une poule passe, jette un regard, puis continue sa route.",
        "Un bruit de piston coincé retentit, puis le silence revient.",
        "Les zombies locaux observent la scène avec un respect inattendu.",
        "Il laisse derrière lui quelques graines et une histoire embarrassante.",
        "Même le golem de fer hoche la tête d'un air grave.",
        "Son ombre met un instant à comprendre qu'il n'est plus là.",
        "La pluie commence pile au moment où il touche le sol.",
        "La terre vibre un peu, comme pour dire merci.",
        "On entend un villageois murmurer : 'Heureusement que c'était pas moi.'",
        "Sa boussole continue de tourner comme si de rien n'était.",
        "Ses particules disparaissent doucement, telles des braises qui s'éteignent.",
        "Une abeille vient inspecter les lieux, puis repart rassurée.",
        "Il laisse un panneau griffonné : 'Je reviens, peut-être.'"
    );

    static {
        WORD_BANKS.put(MobTheme.VISQUEUX, new WordBank(
            List.of("Glu", "Bavi", "Crapou", "Moulu", "Dégou", "Crado", "Soupi", "Patra", "Boue"),
            List.of("bi", "pi", "ti", "po", "gro", "mou", "plu", "bari", "glou", "prou"),
            List.of("fange", "grume", "gnolle", "nouille", "boulette", "patte", "bave", "bouillasse", "torche", "soupe"),
            "Visqueux"
        ));
        WORD_BANKS.put(MobTheme.ENFLAMME, new WordBank(
            List.of("Flam", "Pyro", "Fumi", "Cendre", "Braise", "Brasero", "Charbo", "Foudre", "Ignis"),
            List.of("bou", "grill", "crou", "fum", "brul", "carb", "flam", "incend", "foud"),
            List.of("ouillasse", "ichon", "etincelle", "ignol", "flammèche", "pathe", "opathe", "scurion", "tison"),
            "Enflammé"
        ));
        WORD_BANKS.put(MobTheme.AERIEN, new WordBank(
            List.of("Tourni", "Coui", "Ventri", "Plume", "Nuagi", "Aéro", "Souf", "Brise", "Flotti"),
            List.of("bour", "flotti", "plum", "vent", "tour", "spir", "nuag", "volett", "plan"),
            List.of("asque", "ard", "icule", "ette", "elette", "ouillard", "inette", "oline", "onique"),
            "Aérien"
        ));
        WORD_BANKS.put(MobTheme.AQUATIQUE, new WordBank(
            List.of("Garg", "Glou", "Moula", "Brume", "Bulle", "Touille", "Flotte", "Riba", "Gouti"),
            List.of("loup", "poul", "glou", "marin", "poiss", "buli", "clapot", "flot", "garg"),
            List.of("ax", "inette", "etang", "ouille", "eline", "ipède", "opathe", "inade", "ogrume"),
            "Aquatique"
        ));
        WORD_BANKS.put(MobTheme.SQUELETTE, new WordBank(
            List.of("Cliquet", "Os", "Tibia", "Fémur", "Craque", "Crâne", "Sec", "Rabou", "Fouet"),
            List.of("tos", "clac", "racla", "grince", "couine", "fém", "osse", "tic", "tac"),
            List.of("tron", "mou", "bouc", "osse", "osselet", "icule", "iboule", "arpe", "ost", "ophane"),
            "Squelettique"
        ));
        WORD_BANKS.put(MobTheme.MAGIQUE, new WordBank(
            List.of("Nocti", "Blor", "Zongo", "Éthé", "Mysti", "Arcano", "Runi", "Astral", "Sorc"),
            List.of("glop", "tib", "mag", "lun", "astr", "arc", "éther", "nimb", "vort"),
            List.of("opathe", "ombre", "oïde", "omancien", "omancien", "omancien II", "omancien du Bide", "omancien supérieur", "omancien mineur"),
            "Magique"
        ));
        WORD_BANKS.put(MobTheme.ABSURDE, new WordBank(
            List.of("Pat", "Torde", "Croqui", "Boudi", "Roncho", "Bouffi", "Fum", "Farfel", "Bidon"),
            List.of("agrum", "glouti", "bid", "gnon", "fufu", "tordu", "gnac", "pipo", "loufo"),
            List.of("ette", "or", "ifère", "gnasse", "gnon", "gnole", "gnon", "uche", "ax"),
            "Absurde"
        ));

        mapTheme(EntityType.ZOMBIE, MobTheme.VISQUEUX);
        mapTheme(EntityType.HUSK, MobTheme.VISQUEUX);
        mapTheme(EntityType.DROWNED, MobTheme.AQUATIQUE);
        mapTheme(EntityType.SLIME, MobTheme.VISQUEUX);
        mapTheme(EntityType.MAGMA_CUBE, MobTheme.ENFLAMME);
        mapTheme(EntityType.SKELETON, MobTheme.SQUELETTE);
        mapTheme(EntityType.WITHER_SKELETON, MobTheme.SQUELETTE);
        mapTheme(EntityType.STRAY, MobTheme.SQUELETTE);
        mapTheme(EntityType.BLAZE, MobTheme.ENFLAMME);
        mapTheme(EntityType.GHAST, MobTheme.AERIEN);
        mapTheme(EntityType.PHANTOM, MobTheme.AERIEN);
        mapTheme(EntityType.ALLAY, MobTheme.AERIEN);
        mapTheme(EntityType.ENDERMAN, MobTheme.MAGIQUE);
        mapTheme(EntityType.ENDERMITE, MobTheme.MAGIQUE);
        mapTheme(EntityType.WITCH, MobTheme.MAGIQUE);
        mapTheme(EntityType.SPIDER, MobTheme.VISQUEUX);
        mapTheme(EntityType.CAVE_SPIDER, MobTheme.VISQUEUX);
        mapTheme(EntityType.GUARDIAN, MobTheme.AQUATIQUE);
        mapTheme(EntityType.ELDER_GUARDIAN, MobTheme.AQUATIQUE);
    }

    private MobFlavor() {
    }

    public static NameBundle generateName(EntityType entityType, MobRarity rarity, Random random) {
        MobTheme theme = THEME_BY_ENTITY.getOrDefault(entityType, MobTheme.ABSURDE);
        WordBank bank = WORD_BANKS.getOrDefault(theme, WORD_BANKS.get(MobTheme.ABSURDE));

        if (rarity.getTier() >= 4 && random.nextDouble() < 0.4) {
            String elite = ELITE_NAMES.get(random.nextInt(ELITE_NAMES.size()));
            return new NameBundle(elite, theme);
        }

        String prefix = bank.prefixes().get(random.nextInt(bank.prefixes().size()));
        String core = bank.cores().get(random.nextInt(bank.cores().size()));
        String suffix = bank.suffixes().get(random.nextInt(bank.suffixes().size()));

        String combined = prefix + core + suffix;
        return new NameBundle(combined, theme);
    }

    public static String randomDeathQuip(Random random) {
        return DEATH_QUIPS.get(random.nextInt(DEATH_QUIPS.size()));
    }

    public static String colorize(String input) {
        if (input == null) {
            return null;
        }
        return input.replace('&', '§');
    }

    private static void mapTheme(EntityType type, MobTheme theme) {
        THEME_BY_ENTITY.putIfAbsent(type, theme);
    }

    public enum MobTheme {
        VISQUEUX("Visqueux"),
        ENFLAMME("Enflammé"),
        AERIEN("Aérien"),
        AQUATIQUE("Aquatique"),
        SQUELETTE("Squelettique"),
        MAGIQUE("Magique"),
        ABSURDE("Absurde");

        private final String label;

        MobTheme(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private record WordBank(List<String> prefixes, List<String> cores, List<String> suffixes, String familyName) {
    }

    public record NameBundle(String displayName, MobTheme theme) {
        public String familyLabel() {
            return theme.getLabel();
        }
    }

    public static Random random() {
        return ThreadLocalRandom.current();
    }
}
