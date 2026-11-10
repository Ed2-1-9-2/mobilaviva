package com.mobila.shop;

import java.text.Normalizer;

/**
 * Ilustrații vectoriale de mobilier (gradienți, umbre, culoarea produsului), generate în server.
 * Fără fișiere externe, ~3-5 KB per imagine, cache-uite de browser.
 */
public final class FurnitureArt {
    private FurnitureArt() {}

    private static final String BOOKS[] = {"#b5493a", "#2f6b8a", "#d6a23f", "#56803c", "#7a5aa6", "#e6dfd0", "#3d3d42"};

    public static String svg(String name, String category, String material, String color) {
        String n = norm(name), cat = norm(category), mat = norm(material);
        String[] cols = (color == null ? "" : color).split("\\+");
        String c = hex(cols[0], "#9e9e9e");
        String a = cols.length > 1 ? hex(cols[1], shade(c, -0.35)) : shade(c, -0.35);
        String d = shade(c, -0.28), l = shade(c, 0.22);
        StringBuilder s = new StringBuilder(4096);
        s.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 280' width='400' height='280'><defs>")
         .append(lin("gw", "#eef0ec", "#dde1db"))
         .append(lin("gf", "#d2bd9f", "#b39b7a"))
         .append(lin("gc", l, c))
         .append(lin("gd", c, d))
         .append(lin("ga", shade(a, 0.2), a))
         .append(lin("gg", "#d7ecf5", "#8fb4c8"))
         .append(lin("gs", "#35597d", "#0f1c2c"))
         .append("<radialGradient id='sh'><stop offset='0' stop-color='#000' stop-opacity='.4'/><stop offset='1' stop-color='#000' stop-opacity='0'/></radialGradient>")
         .append("<radialGradient id='gl'><stop offset='0' stop-color='#fff6d0' stop-opacity='.95'/><stop offset='1' stop-color='#fff6d0' stop-opacity='0'/></radialGradient>")
         .append("<radialGradient id='vg' cx='.5' cy='.4' r='.75'><stop offset='.5' stop-color='#000' stop-opacity='0'/><stop offset='1' stop-color='#000' stop-opacity='.18'/></radialGradient>")
         .append("</defs>")
         .append(r(0, 0, 400, 280, 0, "url(#gw)"))
         .append(r(0, 212, 400, 68, 0, "url(#gf)"))
         .append(r(0, 209, 400, 4, 0, "#00000014"))
         .append("<rect x='0' y='0' width='400' height='209' fill='url(#vg)'/>")
         .append("<rect x='0' y='201' width='400' height='9' fill='#fff' opacity='.6'/><rect x='0' y='209' width='400' height='2' fill='#000' opacity='.12'/>")
         .append("<path d='M0 224H400M0 240H400M0 258H400M70 213V224M250 213V224M160 224V240M330 224V240M20 240V258M210 240V258M120 258V280M300 258V280' stroke='#000' stroke-opacity='.09' stroke-width='1.2'/>")
         .append("<polygon points='0,212 150,212 90,280 0,280' fill='#fff' opacity='.10'/>");
        draw(s, kind(n, cat, mat), n, mat, c, d, l, a);
        return fixAlpha(s.append("</svg>").toString());
    }

    // ------------------------------------------------------------------ tip produs
    private static String kind(String n, String cat, String mat) {
        if (cat.equals("scaune")) {
            boolean setul = n.startsWith("set ") || n.contains("(set");
            if (n.contains("gaming")) return "o_gaming";
            if (n.contains("executive") || n.contains("conferinta")) return "o_executive";
            if (n.contains("posture") || (n.contains("ergonomic") && n.contains("tetier"))) return "o_ergonomic";
            if (n.contains("birou") || n.contains("ergonomic")) return "o_mesh";
            if (!setul && n.startsWith("taburet")) return "pouf";
            if (!setul && n.startsWith("scaun")) {
                if (n.contains(" bar ")) return "barchair";
                if (n.contains("windsor")) return "dc_windsor";
                if (n.contains("velvet") || n.contains("catifea")) return "dc_velvet";
                if (n.contains("boucle") || n.contains("cloud")) return "dc_boucle";
                if (n.contains("piele") || n.contains("cognac")) return "dc_leather";
                if (n.contains("rattan") || n.contains("ratan") || n.contains("bali")) return "dc_rattan";
                if (n.contains("shell")) return "dc_shell";
                if (n.contains("tolix") || n.contains("loft") || n.contains("metalic")) return "dc_tolix";
                if (n.contains("pliabil")) return "dc_folding";
                return "dc_scandi";
            }
        }
        if (n.contains("coltar")) return "corner";
        if (n.contains("fotoliu")) return "armchair";
        if (n.contains("saltea")) return "mattress";
        if (n.contains("bibliotec")) return "bookshelf";
        if (n.contains("noptier")) return "nightstand";
        if (n.contains("comoda tv") || n.contains(" tv")) return "tvunit";
        if (n.contains("masuta")) return "coffee";
        if (n.contains("lustra")) return "chandelier";
        if (n.contains("lampadar")) return "floorlamp";
        if (cat.equals("canapele") || n.contains("canapea")) return "sofa";
        if (cat.equals("paturi") || n.startsWith("pat ")) return "bed";
        if (cat.equals("dulapuri") || n.contains("dulap")) return "wardrobe";
        if (cat.equals("comode")) return "dresser";
        if (cat.equals("mese")) return "table";
        if (cat.equals("scaune")) return n.contains("bar") ? "barstool" : n.contains("birou") ? "officechair" : "chairs";
        if (cat.equals("birouri")) return "desk";
        if (cat.equals("iluminat")) return "floorlamp";
        if (n.contains("birou")) return "desk";
        if (n.contains("bibliotec")) return "bookshelf";
        if (n.contains("dulap")) return "wardrobe";
        return "dresser";
    }

    private static void draw(StringBuilder s, String k, String n, String mat, String c, String d, String l, String a) {
        if (k.startsWith("dc_")) { String st = k.substring(3); diningChair(s, st, c, d, l, a, st.equals("velvet") || st.equals("leather") || st.equals("tolix") || st.equals("folding")); return; }
        if (k.startsWith("o_")) { officeSeat(s, k.substring(2), c, d, l, a); return; }
        if (k.equals("barchair")) { barChair(s, c, d, a); return; }
        if (k.equals("pouf")) { pouf(s, c, l); return; }
        switch (k) {
            case "sofa" -> sofa(s, a, false);
            case "corner" -> sofa(s, a, true);
            case "armchair" -> armchair(s, a);
            case "bed" -> bed(s, mat, d, a);
            case "mattress" -> mattress(s, a);
            case "wardrobe" -> wardrobe(s, n.contains("glisant"));
            case "bookshelf" -> bookshelf(s);
            case "tvunit" -> tvunit(s);
            case "nightstand" -> nightstand(s, a);
            case "dresser" -> dresser(s, mat);
            case "coffee" -> coffee(s, a, mat);
            case "table" -> table(s, n, mat, a);
            case "chairs" -> chairs(s, a);
            case "barstool" -> barstool(s);
            case "officechair" -> officechair(s, l);
            case "desk" -> desk(s, n.contains("electric"), a);
            case "chandelier" -> chandelier(s, a);
            default -> floorlamp(s, c);
        }
    }

    // ------------------------------------------------------------------ desene
    private static void sofa(StringBuilder s, String a, boolean corner) {
        int w = corner ? 320 : 272, x0 = corner ? 40 : 64;
        s.append(shadow(200, w + 40));
        s.append(r(x0, 96, w, 78, 20, "url(#gd)"));
        int cushions = corner ? 3 : 2, cw = (w - 40) / cushions;
        for (int i = 0; i < cushions; i++) s.append(r(x0 + 16 + i * (cw + 4), 104, cw - 4, 52, 14, "url(#gc)"));
        s.append(r(x0 - 14, 128, 38, 78, 15, "url(#gc)"));
        s.append(r(x0 + w - 24, 128, 38, 78, 15, "url(#gc)"));
        s.append(r(x0 + 8, 158, w - 16, 36, 8, "url(#gd)"));
        int sw = corner ? 208 : w - 48;
        for (int i = 0; i < cushions && !corner; i++) s.append(r(x0 + 22 + i * (sw / 2 + 2), 146, sw / 2 - 4, 32, 10, "url(#gc)"));
        if (corner) {
            s.append(r(x0 + 22, 146, 98, 32, 10, "url(#gc)")).append(r(x0 + 124, 146, 98, 32, 10, "url(#gc)"));
            s.append(r(x0 + 214, 168, 116, 40, 12, "url(#gc)")).append(r(x0 + 210, 192, 124, 14, 6, "url(#gd)"));
        }
        s.append(r(x0 + 30, 118, 34, 34, 8, "url(#ga)", "rotate(-10 " + (x0 + 47) + " 135)"));
        s.append(r(x0 - 2, 202, 12, 10, 2, "#3b2a1e")).append(r(x0 + w - 10, 202, 12, 10, 2, "#3b2a1e"));
    }

    private static void armchair(StringBuilder s, String a) {
        s.append(shadow(200, 190));
        s.append(r(136, 78, 128, 104, 24, "url(#gd)"));
        s.append(r(150, 86, 100, 66, 16, "url(#gc)"));
        s.append(r(118, 122, 34, 84, 15, "url(#gc)")).append(r(248, 122, 34, 84, 15, "url(#gc)"));
        s.append(r(138, 160, 124, 34, 8, "url(#gd)")).append(r(152, 146, 96, 34, 10, "url(#gc)"));
        s.append(r(172, 108, 30, 30, 8, "url(#ga)", "rotate(8 187 123)"));
        s.append(r(128, 202, 10, 10, 2, "#3b2a1e")).append(r(262, 202, 10, 10, 2, "#3b2a1e"));
    }

    private static void bed(StringBuilder s, String mat, String d, String a) {
        s.append(shadow(200, 300));
        if (mat.contains("metal")) {
            s.append(r(86, 66, 7, 146, 3, "#1d1d1f")).append(r(307, 66, 7, 146, 3, "#1d1d1f")).append(r(86, 66, 228, 7, 3, "#1d1d1f"));
            for (int x = 108; x < 300; x += 22) s.append(r(x, 73, 4, 80, 2, "#2a2a2d"));
        } else {
            s.append(r(84, 60, 232, 118, 16, "url(#gd)")).append(r(96, 72, 208, 94, 10, "url(#gc)"));
            if (mat.contains("stofa")) for (int x = 128; x < 290; x += 36) for (int y = 92; y < 150; y += 28)
                s.append(ell(x + 6, y + 6, 3, 3, d));
            else for (int x = 120; x < 290; x += 34) s.append(r(x, 72, 2, 94, 1, "#00000022"));
        }
        s.append(r(80, 178, 240, 28, 6, "url(#gd)")).append(r(88, 204, 10, 8, 2, "#2b2b2b")).append(r(302, 204, 10, 8, 2, "#2b2b2b"));
        s.append(r(84, 150, 232, 36, 10, "#f6f3ec"));
        s.append(r(84, 164, 232, 28, 8, "url(#ga)")).append(r(84, 164, 232, 7, 4, "#ffffff40"));
        s.append(r(100, 138, 88, 28, 14, "#ffffff", null, "#d8d3c6")).append(r(212, 138, 88, 28, 14, "#ffffff", null, "#d8d3c6"));
    }

    private static void mattress(StringBuilder s, String a) {
        s.append(shadow(200, 300));
        s.append(r(68, 124, 264, 42, 16, "#fbf9f4")).append(r(68, 158, 264, 36, 10, "#e7e2d6"));
        s.append("<path d='M78 146H322' stroke='#d0c9b8' stroke-width='2' stroke-dasharray='7 6' fill='none'/>");
        s.append("<path d='M76 178H324' stroke='#cfc8b8' stroke-width='2' stroke-dasharray='7 6' fill='none'/>");
        s.append(r(300, 168, 20, 10, 2, a));
        s.append(r(80, 194, 240, 12, 3, "#8a6a47")).append(r(92, 204, 12, 8, 2, "#5a4630")).append(r(296, 204, 12, 8, 2, "#5a4630"));
    }

    private static void wardrobe(StringBuilder s, boolean sliding) {
        s.append(shadow(200, 250));
        s.append(r(96, 22, 208, 184, 4, "url(#gd)"));
        int doors = sliding ? 4 : 2, w = 200 / doors;
        for (int i = 0; i < doors; i++) {
            int x = 100 + i * w;
            s.append(r(x, 27, w - 2, 174, 2, "url(#gc)", null, "#c9c2b4"));
            if (sliding && i % 2 == 1) s.append(r(x + 7, 36, w - 16, 156, 2, "url(#gg)"))
                .append("<path d='M" + (x + 12) + " 120L" + (x + w - 14) + " 60' stroke='#fff' stroke-opacity='.5' stroke-width='6'/>");
            s.append(r(i % 2 == 0 ? x + w - 10 : x + 5, 108, 3, 30, 1, "#d9d9d9"));
        }
        s.append(r(100, 204, 200, 8, 2, "#2b2b2b"));
    }

    private static void bookshelf(StringBuilder s) {
        s.append(shadow(200, 190));
        s.append(r(124, 20, 152, 192, 3, "url(#gd)")).append(r(132, 28, 136, 176, 1, "#00000030"));
        int[] boards = {62, 98, 134, 170, 202};
        int top = 28;
        for (int b = 0; b < boards.length; b++) {
            int x = 136, i = 0;
            while (x < 262) {
                int bw = 8 + (i * 5 + b * 3) % 7, h = 16 + (i * 5 + b * 3) % 10;
                if (i % 7 == 6) { x += 14; i++; continue; }
                s.append(r(x, boards[b] - h, bw, h, 1, BOOKS[(i + b * 2) % BOOKS.length]));
                x += bw + 1; i++;
            }
            s.append(r(132, boards[b], 136, 6, 0, "url(#gc)"));
            top = boards[b] + 6;
        }
        s.append(r(124, 206, 152, 6, 2, "#2b2b2b"));
    }

    private static void tvunit(StringBuilder s) {
        s.append(shadow(200, 330));
        s.append(r(102, 44, 196, 88, 5, "#0f1013")).append(r(107, 49, 186, 78, 2, "url(#gs)"))
         .append("<path d='M107 49H190L150 127H107Z' fill='#fff' fill-opacity='.07'/>")
         .append(r(180, 128, 40, 4, 2, "#222"));
        s.append(r(48, 136, 304, 64, 6, "url(#gd)")).append(r(44, 130, 312, 10, 3, "url(#gc)"));
        s.append(r(56, 146, 86, 46, 3, "url(#gc)")).append(r(258, 146, 86, 46, 3, "url(#gc)")).append(r(146, 146, 108, 46, 2, "#00000035"));
        s.append(r(92, 166, 14, 3, 1, "#d9d9d9")).append(r(294, 166, 14, 3, 1, "#d9d9d9")).append(r(166, 172, 44, 20, 2, "#202226")).append(r(216, 178, 26, 14, 2, "#2f3136"));
        s.append(r(62, 200, 9, 12, 2, "#3b2a1e")).append(r(329, 200, 9, 12, 2, "#3b2a1e"));
    }

    private static void nightstand(StringBuilder s, String a) {
        s.append(shadow(200, 150));
        s.append(r(194, 100, 12, 20, 2, "#555")).append(r(196, 112, 8, 10, 2, "#555"));
        s.append("<path d='M166 62H234L246 102H154Z' fill='#f3e9c9'/><path d='M166 62H234L240 80H160Z' fill='#fff' fill-opacity='.35'/>");
        s.append(r(182, 118, 36, 5, 2, "url(#ga)"));
        s.append(r(144, 124, 112, 80, 5, "url(#gd)")).append(r(138, 120, 124, 9, 3, "url(#gc)"));
        s.append(r(150, 134, 100, 40, 3, "url(#gc)")).append(r(188, 150, 24, 3, 1, "#d9d9d9")).append(r(150, 178, 100, 22, 2, "#00000035"));
        s.append(r(150, 204, 8, 8, 2, a)).append(r(242, 204, 8, 8, 2, a));
    }

    private static void dresser(StringBuilder s, String mat) {
        s.append(shadow(200, 270));
        s.append(r(84, 90, 232, 116, 6, "url(#gd)")).append(r(78, 82, 244, 12, 4, "url(#gc)"));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 2; col++) {
            int x = 92 + col * 110, y = 100 + row * 34;
            s.append(r(x, y, 106, 30, 3, "url(#gc)", null, "#c9c2b4")).append(r(x + 43, y + 13, 20, 3, 1, "#dcdcdc"));
        }
        s.append(r(92, 206, 9, 6, 2, "#3b2a1e")).append(r(299, 206, 9, 6, 2, "#3b2a1e"));
    }

    private static void coffee(StringBuilder s, String a, String mat) {
        s.append(shadow(200, 250));
        s.append("<path d='M140 146L128 208M260 146L272 208M200 152V210' stroke='" + a + "' stroke-width='5' stroke-linecap='round'/>");
        s.append(ell(200, 190, 78, 14, "#cfdde3", 0.7, a));
        s.append(ell(200, 134, 108, 22, "#8fb0c1")).append(ell(200, 128, 108, 22, "url(#gg)", 0.92, a));
        s.append("<path d='M120 124Q170 112 230 118' stroke='#fff' stroke-opacity='.7' stroke-width='4' fill='none' stroke-linecap='round'/>");
    }

    private static void table(StringBuilder s, String n, String mat, String a) {
        s.append(shadow(200, 320));
        s.append(r(96, 62, 50, 70, 10, "url(#ga)")).append(r(254, 62, 50, 70, 10, "url(#ga)"));
                s.append(r(70, 118, 260, 12, 2, "url(#gd)"));
        s.append(r(74, 128, 14, 84, 3, "url(#gd)")).append(r(312, 128, 14, 84, 3, "url(#gd)"));
        s.append(r(50, 102, 300, 18, 5, "url(#gc)", null, "#c9c2b4")).append(r(50, 102, 300, 5, 3, "#ffffff35"));
        if (mat.contains("lemn")) for (int x = 70; x < 340; x += 55) s.append("<path d='M" + x + " 112H" + (x + 40) + "' stroke='#0000001c' stroke-width='2'/>");
        if (n.contains("extensibil")) s.append("<path d='M200 102V120' stroke='#00000050' stroke-width='2'/>");
    }

    private static void chair(StringBuilder s, int x, String a) {
        s.append(r(x + 14, 126, 7, 86, 3, shade(a, -0.25))).append(r(x + 79, 126, 7, 86, 3, shade(a, -0.25)));
        s.append(r(x + 10, 52, 80, 80, 14, "url(#gd)")).append(r(x + 18, 60, 64, 64, 10, "url(#gc)"));
        s.append(r(x + 2, 130, 96, 24, 8, "url(#gc)")).append(r(x + 6, 152, 88, 6, 2, "url(#gd)"));
        s.append(r(x + 8, 156, 9, 56, 3, a)).append(r(x + 83, 156, 9, 56, 3, a));
    }

    private static void chairs(StringBuilder s, String a) {
        s.append(shadow(200, 300));
        chair(s, 66, a);
        chair(s, 234, a);
    }

    private static void barstool(StringBuilder s) {
        s.append(shadow(200, 280));
        for (int x : new int[]{96, 244}) {
            s.append("<path d='M" + (x + 14) + " 86L" + (x + 6) + " 210M" + (x + 46) + " 86L" + (x + 54) + " 210' stroke='#1c1c1e' stroke-width='4' stroke-linecap='round'/>");
            s.append("<ellipse cx='" + (x + 30) + "' cy='168' rx='25' ry='5' fill='none' stroke='#1c1c1e' stroke-width='3'/>");
            s.append(r(x + 4, 78, 52, 6, 3, "#222")).append(r(x - 2, 62, 64, 18, 9, "url(#gc)"));
        }
    }

    private static void officechair(StringBuilder s, String l) {
        s.append(shadow(200, 150));
        s.append(r(196, 120, 8, 24, 2, "#2a2a2d"));
        s.append(r(146, 28, 108, 98, 28, "#26262a"));
        for (int y = 40; y < 118; y += 9) s.append("<path d='M158 " + y + "H242' stroke='" + l + "' stroke-opacity='.45' stroke-width='2'/>");
        s.append(r(118, 108, 8, 36, 3, "#1c1c1e")).append(r(274, 108, 8, 36, 3, "#1c1c1e"));
        s.append(r(108, 104, 28, 7, 3, "#1c1c1e")).append(r(264, 104, 28, 7, 3, "#1c1c1e"));
        s.append(r(136, 140, 128, 26, 13, "url(#gc)")).append(r(196, 166, 8, 30, 2, "#8c9096"));
        s.append("<path d='M200 194L142 207M200 194L258 207M200 194L170 210M200 194L230 210' stroke='#1c1c1e' stroke-width='6' stroke-linecap='round'/>");
        for (int x : new int[]{140, 170, 230, 260}) s.append("<circle cx='" + x + "' cy='" + (x == 170 || x == 230 ? 209 : 207) + "' r='5' fill='#0e0e10'/>");
    }

    private static void desk(StringBuilder s, boolean electric, String a) {
        s.append(shadow(200, 320));
        s.append(r(136, 34, 128, 66, 5, "#0f1013")).append(r(141, 39, 118, 56, 2, "url(#gs)"))
         .append("<path d='M141 39H210L180 95H141Z' fill='#fff' fill-opacity='.07'/>")
         .append(r(194, 100, 12, 6, 2, "#555")).append(r(176, 106, 48, 4, 2, "#444"));
        if (electric) {
            s.append(r(84, 124, 12, 82, 2, "#1d1d1f")).append(r(304, 124, 12, 82, 2, "#1d1d1f"));
            s.append(r(66, 206, 48, 6, 3, "#1d1d1f")).append(r(286, 206, 48, 6, 3, "#1d1d1f")).append(r(90, 124, 220, 6, 2, "#1d1d1f"));
        } else {
            s.append(r(58, 124, 10, 88, 2, a));
            s.append(r(236, 124, 104, 88, 3, "url(#gd)"));
            for (int i = 0; i < 3; i++) s.append(r(242, 130 + i * 26, 92, 22, 2, "url(#gc)", null, "#c9c2b4")).append(r(278, 139 + i * 26, 20, 3, 1, "#dcdcdc"));
        }
        s.append(r(50, 110, 300, 15, 3, "url(#gc)", null, "#c9c2b4")).append(r(50, 110, 300, 4, 2, "#ffffff35"));
        s.append(r(100, 99, 22, 11, 3, "#e8e2d4")).append(r(280, 96, 5, 14, 2, "#3e7d4f")).append(r(274, 92, 5, 18, 2, "#4e9361"));
    }

    private static void chandelier(StringBuilder s, String a) {
        s.append(r(198, 0, 4, 56, 0, "#4b4b4b")).append(r(184, 0, 32, 8, 3, "url(#ga)"));
        s.append("<path d='M200 56L120 98M200 56L280 98M200 56V108' stroke='" + a + "' stroke-width='3' fill='none'/>");
        s.append("<ellipse cx='200' cy='100' rx='94' ry='15' fill='none' stroke='" + a + "' stroke-width='5'/>");
        s.append("<ellipse cx='200' cy='100' rx='70' ry='10' fill='none' stroke='" + a + "' stroke-width='2'/>");
        for (int x : new int[]{126, 163, 200, 237, 274}) {
            int y = 98 + (x == 200 ? 14 : (x == 163 || x == 237) ? 11 : 2);
            s.append("<circle cx='" + x + "' cy='" + y + "' r='28' fill='url(#gl)'/><circle cx='" + x + "' cy='" + y + "' r='8' fill='#fff3c4'/>");
        }
        s.append(shadow(200, 120));
    }

    private static void floorlamp(StringBuilder s, String c) {
        s.append(shadow(120, 100));
        s.append("<path d='M120 206V80Q120 30 200 30Q282 30 282 84' stroke='" + c + "' stroke-width='5' fill='none' stroke-linecap='round'/>");
        s.append(r(92, 204, 56, 8, 4, "#222"));
        s.append("<path d='M252 80H312L298 122H266Z' fill='#f1e6c8'/><path d='M252 80H312L308 94H256Z' fill='#fff' fill-opacity='.4'/>");
        s.append(ell(282, 124, 18, 5, "#ffe8a0")).append("<circle cx='282' cy='130' r='30' fill='url(#gl)' opacity='.6'/>");
    }

    // ------------------------------------------------------------------ scaune individuale (vedere 3/4, materiale și umbre mai realiste)
    private static String g(String id, String top, String bot) { return lin(id, top, bot); }
    private static String poly(String pts, String fill) { return "<polygon points='" + pts + "' fill='" + fill + "'/>"; }
    private static String line(int x1, int y1, int x2, int y2, String col, double w) {
        return "<line x1='" + x1 + "' y1='" + y1 + "' x2='" + x2 + "' y2='" + y2 + "' stroke='" + col + "' stroke-width='" + w + "' stroke-linecap='round'/>";
    }
    private static String foot(int x, int y) { return "<ellipse cx='" + x + "' cy='" + y + "' rx='9' ry='3' fill='#000' opacity='.28'/>"; }

    /** Scaun de dining în proiecție oblică: stil = windsor | scandi | velvet | boucle | leather | rattan | shell | tolix | folding. */
    private static void diningChair(StringBuilder s, String style, String c, String d, String l, String a, boolean metalLegs) {
        String legCol = style.equals("velvet") ? "#c9a24a" : "#2c2c30";
        String legDark = metalLegs ? (style.equals("velvet") ? "#8f7430" : "#161618") : "#6f4a27";
        boolean woodBody = style.equals("windsor") || style.equals("scandi");
        s.append("<defs>").append(g("wl", woodBody ? shade(c, 0.12) : "#c28a4e", woodBody ? shade(c, -0.2) : "#8d5e31")).append(g("wd", woodBody ? shade(c, -0.3) : "#8a5a2e", woodBody ? shade(c, -0.5) : "#5c3a1c")).append("</defs>");
        s.append(shadow(212, 230));
        boolean wood = !metalLegs;
        String lf = wood ? "url(#wl)" : legCol, ld = wood ? "url(#wd)" : legDark;
        // picioare din spate (mai întunecate)
        s.append(poly("176,112 184,112 180,194 174,194", ld)).append(poly("268,112 276,112 274,194 268,194", ld));
        s.append(foot(177, 195)).append(foot(271, 195));
        // spătar: montanți + panou
        if (style.equals("windsor")) {
            s.append(poly("176,112 184,112 186,30 178,30", ld)).append(poly("268,112 276,112 274,30 266,30", ld));
            for (int i = 0; i < 6; i++) { int x = 192 + i * 14; s.append(line(x, 112, x, 40, shade(c, 0.05), 5)).append(line(x - 1, 112, x - 1, 42, "#ffffff40", 1.5)); }
            s.append("<rect x='172' y='26' width='108' height='16' rx='7' fill='url(#wl)'/><rect x='172' y='26' width='108' height='4' rx='2' fill='#fff' opacity='.28'/>");
        } else if (style.equals("tolix")) {
            s.append(poly("176,112 183,112 185,44 178,44", ld)).append(poly("269,112 276,112 274,44 267,44", ld));
            s.append("<path d='M176 62 Q226 34 276 62 L274 90 Q226 66 178 90Z' fill='" + c + "'/>")
             .append("<path d='M176 62 Q226 34 276 62' fill='none' stroke='#fff' stroke-opacity='.35' stroke-width='3'/>");
            for (int i = 0; i < 5; i++) s.append("<circle cx='" + (196 + i * 15) + "' cy='" + (66 - (i == 2 ? 6 : (i == 1 || i == 3) ? 3 : -1)) + "' r='2.3' fill='#000' opacity='.35'/>");
        } else if (style.equals("shell")) {
            s.append("<path d='M168 118 Q158 40 226 30 Q296 40 282 118 Q226 100 168 118Z' fill='url(#gd)' stroke='#000' stroke-opacity='.22' stroke-width='1.5'/>")
             .append("<path d='M180 110 Q172 50 226 41 Q280 50 272 110 Q226 98 180 110Z' fill='url(#gc)'/>")
             .append("<path d='M184 100 Q180 56 226 47' fill='none' stroke='#fff' stroke-opacity='.55' stroke-width='5' stroke-linecap='round'/>");
        } else if (style.equals("rattan")) {
            s.append(poly("176,112 184,112 186,66 178,66", ld)).append(poly("268,112 276,112 274,66 266,66", ld));
            s.append("<rect x='174' y='34' width='104' height='80' rx='40' fill='" + legDark + "'/><rect x='181' y='41' width='90' height='66' rx='33' fill='#d9b979'/>");
            s.append("<clipPath id='rc'><rect x='181' y='41' width='90' height='66' rx='33'/></clipPath><g clip-path='url(#rc)' stroke='#9c7a3f' stroke-width='1.6' opacity='.8'>");
            for (int i = -6; i < 14; i++) s.append("<path d='M" + (181 + i * 9) + " 41L" + (181 + i * 9 + 40) + " 107M" + (181 + i * 9 + 40) + " 41L" + (181 + i * 9) + " 107'/>");
            s.append("</g>");
        } else if (style.equals("scandi")) {
            s.append(poly("176,112 184,112 188,36 180,36", ld)).append(poly("268,112 276,112 272,36 264,36", ld));
            s.append("<path d='M172 40 Q226 24 280 40 L278 62 Q226 46 174 62Z' fill='url(#wl)'/>")
             .append("<path d='M174 70 Q226 56 278 70 L276 84 Q226 70 176 84Z' fill='url(#wl)'/>");
        } else if (style.equals("folding")) {
            s.append("<rect x='174' y='44' width='104' height='50' rx='6' fill='" + c + "'/><rect x='174' y='44' width='104' height='7' rx='3' fill='#fff' opacity='.3'/>");
            s.append(line(178, 112, 180, 90, "#2c2c30", 5)).append(line(272, 112, 270, 90, "#2c2c30", 5));
        } else {   // velvet | boucle | leather: spătar tapițat
            String back = style.equals("velvet") ? c : style.equals("leather") ? c : l;
            s.append("<path d='M172 112 L176 40 Q226 24 278 40 L276 112Z' fill='" + shade(back, -0.25) + "'/>");
            s.append("<path d='M178 106 L181 44 Q226 31 273 44 L271 106Z' fill='url(#gc)'/>");
            if (style.equals("velvet")) for (int i = 1; i < 4; i++) s.append(line(178 + i * 24, 44 + (i == 2 ? 0 : 2), 178 + i * 24, 104, "#000", 1.4)).append("<line x1='" + (178 + i * 24) + "' y1='46' x2='" + (178 + i * 24) + "' y2='104' stroke='#fff' stroke-opacity='.18' stroke-width='1.2'/>");
            if (style.equals("leather")) for (int i = 0; i < 3; i++) s.append("<circle cx='" + (200 + i * 26) + "' cy='" + (74 - (i == 1 ? 3 : 0)) + "' r='2.6' fill='#000' opacity='.4'/>");
            if (style.equals("boucle")) for (int k = 0; k < 60; k++) s.append("<circle cx='" + (184 + (k * 37) % 86) + "' cy='" + (50 + (k * 53) % 52) + "' r='2.2' fill='#fff' opacity='.28'/>");
            s.append("<path d='M181 48 Q226 36 271 48' fill='none' stroke='#fff' stroke-opacity='.4' stroke-width='3' stroke-linecap='round'/>");
        }
        // șezut
        String seatTop = style.equals("windsor") ? "url(#wl)" : style.equals("tolix") ? c : "url(#gc)";
        String seatFront = style.equals("windsor") ? "url(#wd)" : style.equals("tolix") ? shade(c, -0.3) : "url(#gd)";
        s.append(poly("148,132 252,132 276,114 172,114", seatTop));
        s.append("<rect x='148' y='132' width='104' height='14' rx='4' fill='" + seatFront + "'/>");
        s.append(poly("252,132 276,114 276,128 252,146", style.equals("windsor") ? "#6f4a27" : shade(c, -0.4)));
        s.append("<path d='M150 134 H250' stroke='#fff' stroke-opacity='.32' stroke-width='2'/>");
        if (style.equals("velvet") || style.equals("boucle") || style.equals("leather") || style.equals("shell") || style.equals("scandi") || style.equals("rattan") || style.equals("folding"))
            s.append("<path d='M156 128 L246 128 L266 116 L180 116Z' fill='#fff' opacity='.14'/>");
        // picioare din față
        s.append(poly("150,144 160,144 154,212 146,212", lf)).append(poly("244,144 254,144 252,212 244,212", lf));
        s.append(foot(150, 213)).append(foot(248, 213));
        if (wood && !style.equals("shell")) {   // stretchere
            s.append(line(152, 184, 248, 184, "#7b5330", 4)).append(line(250, 182, 273, 168, "#6f4a27", 4));
        }
        if (style.equals("folding")) s.append(line(154, 150, 246, 206, "#2c2c30", 3)).append(line(246, 150, 154, 206, "#2c2c30", 3));
        s.append("<path d='M151 150V208' stroke='#fff' stroke-opacity='.25' stroke-width='1.5'/>");
    }

    private static void barChair(StringBuilder s, String c, String d, String a) {
        s.append(shadow(200, 170));
        // picioare spate / față
        String m = "#222226";
        s.append(line(170, 92, 150, 210, m, 6)).append(line(230, 92, 250, 210, m, 6)).append(line(186, 90, 176, 192, "#141416", 5)).append(line(214, 90, 224, 192, "#141416", 5));
        s.append("<ellipse cx='200' cy='150' rx='46' ry='9' fill='none' stroke='#3a3a40' stroke-width='5'/>").append("<ellipse cx='200' cy='148' rx='46' ry='7' fill='none' stroke='#8c9096' stroke-opacity='.45' stroke-width='1.5'/>");
        s.append(foot(150, 212)).append(foot(250, 212)).append(foot(176, 193)).append(foot(224, 193));
        // spătar jos
        s.append("<path d='M156 70 Q200 40 244 70 L240 86 Q200 62 160 86Z' fill='url(#gd)'/>").append("<path d='M158 70 Q200 42 242 70' fill='none' stroke='#fff' stroke-opacity='.35' stroke-width='3'/>");
        // șezut
        s.append("<ellipse cx='200' cy='92' rx='58' ry='15' fill='" + shade(c, -0.45) + "'/>").append("<rect x='142' y='80' width='116' height='12' fill='" + shade(c, -0.4) + "'/>")
         .append("<ellipse cx='200' cy='80' rx='58' ry='15' fill='url(#gc)'/>").append("<ellipse cx='190' cy='76' rx='30' ry='6' fill='#fff' opacity='.22'/>");
    }

    private static void pouf(StringBuilder s, String c, String l) {
        s.append(shadow(200, 170));
        s.append(line(158, 160, 150, 206, "#a9743f", 7)).append(line(242, 160, 250, 206, "#a9743f", 7)).append(line(184, 164, 180, 200, "#7a5230", 6)).append(line(216, 164, 220, 200, "#7a5230", 6));
        s.append(foot(150, 208)).append(foot(250, 208));
        s.append("<ellipse cx='200' cy='156' rx='66' ry='16' fill='" + shade(c, -0.4) + "'/>").append("<rect x='134' y='96' width='132' height='60' fill='url(#gd)'/>")
         .append("<ellipse cx='200' cy='96' rx='66' ry='18' fill='url(#gc)'/>");
        for (int k = 0; k < 110; k++) { int x = 138 + (k * 47) % 124, y = 98 + (k * 31) % 56; s.append("<circle cx='" + x + "' cy='" + y + "' r='2.4' fill='#fff' opacity='.22'/>"); }
        s.append("<ellipse cx='188' cy='92' rx='34' ry='7' fill='#fff' opacity='.3'/>");
    }

    /** scaune de birou: mesh | executive | gaming | ergonomic */
    private static void officeSeat(StringBuilder s, String type, String c, String d, String l, String a) {
        s.append("<defs><pattern id='mp' width='7' height='7' patternUnits='userSpaceOnUse' patternTransform='rotate(45)'><rect width='7' height='7' fill='#2b2d31'/><path d='M0 0V7' stroke='#4a4d54' stroke-width='2'/></pattern>")
         .append(g("lt", shade(c, 0.25), shade(c, -0.2))).append("</defs>");
        s.append(shadow(200, 190));
        // bază stea cu 5 brațe + rotile
        s.append(r(194, 160, 12, 34, 3, "url(#ga)")).append("<rect x='194' y='160' width='4' height='34' fill='#fff' opacity='.25'/>");
        String base = type.equals("executive") ? "#b8bcc2" : "#1c1c1f";
        s.append("<path d='M200 194 L132 206 M200 194 L268 206 M200 194 L158 212 M200 194 L242 212 M200 194 L200 214' stroke='" + base + "' stroke-width='7' stroke-linecap='round'/>");
        for (int[] w : new int[][]{{132, 207}, {268, 207}, {158, 212}, {242, 212}, {200, 214}}) s.append("<circle cx='" + w[0] + "' cy='" + w[1] + "' r='5' fill='#0d0d0f'/><circle cx='" + w[0] + "' cy='" + w[1] + "' r='1.8' fill='#777'/>");
        // șezut
        boolean g = type.equals("gaming");
        s.append("<path d='M130 146 Q200 166 270 146 L262 164 Q200 180 138 164Z' fill='" + shade(c, -0.45) + "'/>");
        s.append("<path d='M134 138 Q200 126 266 138 Q270 150 262 152 Q200 168 138 152 Q130 148 134 138Z' fill='url(#lt)'/>");
        s.append("<path d='M146 140 Q200 130 254 140' fill='none' stroke='#fff' stroke-opacity='.4' stroke-width='3' stroke-linecap='round'/>");
        if (g) s.append("<path d='M136 142 L146 160 M264 142 L254 160' stroke='" + a + "' stroke-width='5' stroke-linecap='round'/>");
        // brațe
        String arm = type.equals("executive") ? "url(#lt)" : "#222226";
        s.append("<rect x='112' y='96' width='8' height='44' rx='3' fill='#1a1a1d'/><rect x='280' y='96' width='8' height='44' rx='3' fill='#1a1a1d'/>");
        s.append("<rect x='" + (type.equals("executive") ? 100 : 104) + "' y='90' width='" + (type.equals("executive") ? 34 : 30) + "' height='" + (type.equals("executive") ? 11 : 7) + "' rx='5' fill='" + arm + "'/>")
         .append("<rect x='" + (type.equals("executive") ? 266 : 266) + "' y='90' width='" + (type.equals("executive") ? 34 : 30) + "' height='" + (type.equals("executive") ? 11 : 7) + "' rx='5' fill='" + arm + "'/>");
        s.append(r(194, 126, 12, 14, 2, "#222226"));
        // spătar
        if (type.equals("mesh") || type.equals("ergonomic")) {
            s.append("<rect x='140' y='26' width='120' height='104' rx='26' fill='#17181b'/>").append("<rect x='148' y='34' width='104' height='88' rx='20' fill='url(#mp)'/>");
            s.append("<path d='M148 34 H206 L170 122 H148Z' fill='#fff' opacity='.06'/>");
            if (type.equals("ergonomic")) { s.append("<rect x='160' y='104' width='80' height='22' rx='10' fill='#17181b'/><rect x='164' y='108' width='72' height='14' rx='7' fill='url(#lt)'/>"); s.append("<rect x='172' y='12' width='56' height='20' rx='9' fill='#17181b'/><rect x='176' y='15' width='48' height='13' rx='6' fill='url(#lt)'/>"); }
        } else if (type.equals("executive")) {
            s.append("<rect x='136' y='46' width='128' height='90' rx='22' fill='" + shade(c, -0.5) + "'/>").append("<rect x='142' y='50' width='116' height='82' rx='18' fill='url(#lt)'/>");
            for (int i = 0; i < 4; i++) s.append("<path d='M146 " + (68 + i * 16) + " Q200 " + (62 + i * 16) + " 254 " + (68 + i * 16) + "' fill='none' stroke='#000' stroke-opacity='.35' stroke-width='2'/>");
            s.append("<rect x='160' y='8' width='80' height='40' rx='16' fill='" + shade(c, -0.5) + "'/><rect x='165' y='12' width='70' height='32' rx='13' fill='url(#lt)'/><path d='M170 18 Q200 12 230 18' fill='none' stroke='#fff' stroke-opacity='.45' stroke-width='3' stroke-linecap='round'/>");
        } else {   // gaming
            s.append("<path d='M138 40 Q200 20 262 40 L268 138 Q200 126 132 138Z' fill='#16161a'/>").append("<path d='M148 46 Q200 30 252 46 L256 128 Q200 118 144 128Z' fill='url(#lt)'/>");
            s.append("<path d='M176 40 L172 128 M224 40 L228 128' stroke='" + a + "' stroke-width='9' opacity='.95'/>").append("<path d='M138 40 L130 98 L146 112 L150 46Z M262 40 L270 98 L254 112 L250 46Z' fill='#0f0f12'/>");
            s.append("<rect x='164' y='6' width='72' height='30' rx='13' fill='" + shade(a, -0.1) + "'/><path d='M170 12 Q200 7 230 12' fill='none' stroke='#fff' stroke-opacity='.4' stroke-width='3' stroke-linecap='round'/>");
            s.append("<rect x='168' y='96' width='64' height='24' rx='11' fill='#111114'/>");
        }
    }

    // ------------------------------------------------------------------ utilitare
    private static String shadow(int cx, int w) { return "<ellipse cx='" + cx + "' cy='213' rx='" + (w / 2) + "' ry='9' fill='url(#sh)'/>"; }
    private static String lin(String id, String a, String b) {
        return "<linearGradient id='" + id + "' x1='0' y1='0' x2='0' y2='1'><stop offset='0' stop-color='" + a + "'/><stop offset='1' stop-color='" + b + "'/></linearGradient>";
    }
    private static String r(int x, int y, int w, int h, int rx, String fill) { return r(x, y, w, h, rx, fill, null, null); }
    private static String r(int x, int y, int w, int h, int rx, String fill, String transform) { return r(x, y, w, h, rx, fill, transform, null); }
    private static String r(int x, int y, int w, int h, int rx, String fill, String transform, String stroke) {
        return "<rect x='" + x + "' y='" + y + "' width='" + w + "' height='" + h + "' rx='" + rx + "' fill='" + fill + "'"
            + (transform != null ? " transform='" + transform + "'" : "") + (stroke != null ? " stroke='" + stroke + "' stroke-width='1.5'" : "") + "/>";
    }
    private static String ell(int cx, int cy, int rx, int ry, String fill) { return ell(cx, cy, rx, ry, fill, 1, null); }
    private static String ell(int cx, int cy, int rx, int ry, String fill, double op, String stroke) {
        return "<ellipse cx='" + cx + "' cy='" + cy + "' rx='" + rx + "' ry='" + ry + "' fill='" + fill + "' opacity='" + op + "'"
            + (stroke != null ? " stroke='" + stroke + "' stroke-width='3'" : "") + "/>";
    }

    /** #RRGGBBAA -> #RRGGBB + opacity (compatibil cu orice randor SVG). */
    private static final java.util.regex.Pattern ALPHA = java.util.regex.Pattern.compile("(fill|stroke)='#([0-9a-fA-F]{6})([0-9a-fA-F]{2})'");
    private static String fixAlpha(String svg) {
        java.util.regex.Matcher m = ALPHA.matcher(svg);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            double op = Integer.parseInt(m.group(3), 16) / 255.0;
            m.appendReplacement(out, m.group(1) + "='#" + m.group(2) + "' " + m.group(1) + "-opacity='" + String.format(java.util.Locale.ROOT, "%.2f", op) + "'");
        }
        m.appendTail(out);
        return out.toString();
    }

    static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
    }

    static String hex(String name, String def) {
        String n = norm(name);
        String[][] m = {{"antracit", "#3a3d42"}, {"galben", "#e0b43a"}, {"albastru petrol", "#2f6f7e"}, {"albastru", "#3b6ea5"}, {"gri", "#8b9099"}, {"maro", "#7a4b2a"}, {"crem", "#eadfc8"},
            {"bej", "#cfbb9a"}, {"stejar", "#b88a55"}, {"negru", "#2b2b2e"}, {"alb", "#f1f1ee"}, {"nuc", "#6b4a33"}, {"auriu", "#c9a24a"},
            {"verde", "#4f7a4a"}, {"rosu", "#a8392f"}};
        for (String[] e : m) if (n.contains(e[0])) return e[1];
        return def;
    }

    static String shade(String hex, double f) {
        int v = Integer.parseInt(hex.substring(1), 16), rgb[] = {(v >> 16) & 255, (v >> 8) & 255, v & 255};
        for (int i = 0; i < 3; i++) rgb[i] = (int) Math.round(f < 0 ? rgb[i] * (1 + f) : rgb[i] + (255 - rgb[i]) * f);
        return String.format("#%02x%02x%02x", rgb[0], rgb[1], rgb[2]);
    }
}
