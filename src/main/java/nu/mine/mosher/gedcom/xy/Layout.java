/*
    Copyright © 2000–2020, Christopher Alan Mosher, Shelton, Connecticut, USA, <cmosher01@gmail.com>.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package nu.mine.mosher.gedcom.xy;

import javafx.geometry.Point2D;
import org.slf4j.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Genealogical automatic intelligent drop-line chart layout algorithm.
 */
public class Layout {
    private static final Logger LOG = LoggerFactory.getLogger(Layout.class);

    public static final double MAX_LEVEL = 5000.0D;
    private static final double GEN_HEIGHT = 108D;
    private static final double MAX_WIDTH = 54D;
    public static final double DX_FAMILY = MAX_WIDTH * 5;

    public Layout(final List<Indi> indis, final List<Fami> famis) {
        this.indis = IntStream.range(0, indis.size())
                .mapToObj(i -> new Individual(indis.get(i), i))
                .collect(Collectors.toList());

        final Map<Indi, Individual> mapIndis = new HashMap<>();
        this.indis.forEach(i -> mapIndis.put(i.indi, i));

        this.famis = IntStream.range(0, famis.size())
                .mapToObj(i -> new Family(famis.get(i), i))
                .collect(Collectors.toList());

        this.famis.forEach(f -> {
            final Fami fami = f.fami;

            final Optional<Indi> husb = fami.getHusb();
            final Optional<Indi> wife = fami.getWife();

            addSpouse(mapIndis, f, husb, wife);
            addSpouse(mapIndis, f, wife, husb);

            fami.getChildren().forEach(c -> {
                final Individual cChild = mapIndis.get(c);
                cChild.idxChildToFamily = f.idx;
                f.children.add(cChild.idx);
                if (husb.isPresent()) {
                    final Individual cHusb = mapIndis.get(husb.get());
                    cHusb.ridxChild.add(mapIndis.get(c).idx);
                    cChild.idxFather = cHusb.idx;
                }
                if (wife.isPresent()) {
                    final Individual cWife = mapIndis.get(wife.get());
                    cWife.ridxChild.add(mapIndis.get(c).idx);
                    cChild.idxMother = cWife.idx;
                }
            });
        });
    }

    private static void addSpouse(Map<Indi, Individual> mapIndis, Family fami, Optional<Indi> indi, Optional<Indi> spouse) {
        if (indi.isPresent()) {
            final Individual cindi = mapIndis.get(indi.get());
            cindi.ridxSpouseToFamily.add(fami.idx);
            spouse.ifPresent(i -> cindi.ridxSpouse.add(mapIndis.get(i).idx));
        }
    }








    private final List<Individual> indis;
    private final List<Family> famis;


    class Family {
        private final int idx;
        private final Fami fami;

        /* indexes into indis list */
        private final List<Integer> children = new ArrayList<>();

        public Family(final Fami fami, final int idx) {
            this.fami = fami;
            this.idx = idx;
        }

        void GetSortedChildren(final List<Integer> riChild) {
            riChild.clear();
            riChild.addAll(children);
            riChild.sort(Comparator.comparing(i -> Layout.this.indis.get(i).getBirthForSort()));
        }
    }


    class Individual {
        private Layout layout = Layout.this;

        private int sex; // 0=unknown, 1=male, 2=female
        private Point2D location = Point2D.ZERO;

        private final Indi indi;
        /* indexes into indis list */
        private final int idx;
        private int idxFather = -1;
        private int idxMother = -1;
        private List<Integer> ridxSpouse = new ArrayList<>();
        private List<Integer> ridxChild = new ArrayList<>();
        /* indexes into famis list */
        private int idxChildToFamily = -1;
        private List<Integer> ridxSpouseToFamily = new ArrayList<>();

        private boolean mark;
        private int level;
        private int maxMale;
        private Individual house;


        public Individual(final Indi indi, final int idx) {
            this.indi = indi;
            this.idx = idx;
            this.sex = indi.getSex();
        }

        void setLevel(final int lev) {
            //already done
            if (this.mark) {
                return;
            }

            //doing
            this.mark = true;

            //self
            this.level = lev;

            // position along y axis
            moveYto((MAX_LEVEL - this.level) * GEN_HEIGHT);

            //father
            if (idxFather >= 0) {
                layout.indis.get(idxFather).setLevel(lev + 1);
            }
            //mother (only if no father)
            else if (idxMother >= 0) {
                layout.indis.get(idxMother).setLevel(lev + 1);
            }

            //siblings
            if (idxChildToFamily >= 0) {
                final Family fami = layout.famis.get(idxChildToFamily);
                for (int i = 0; i < fami.children.size(); ++i) {
                    layout.indis.get(fami.children.get(i)).setLevel(lev);
                }
            }

            //children
            for (final Integer i : ridxChild) {
                layout.indis.get(i).setLevel(lev - 1);
            }

            //spouses
            for (final Integer i : ridxSpouse) {
                layout.indis.get(i).setLevel(lev);
            }
        }

        void moveXto(final double x) {
            this.location = new Point2D(x, this.location.getY());
        }

        void moveYto(final double y) {
            this.location = new Point2D(this.location.getX(), y);
        }

        void setMaxMaleIf(final int n) {
            if (maxMale < n) {
                maxMale = n;
            }
        }

        void setRootWithSpouses(final Individual proot) {
            if (mark) {
                return;
            }

            final List<Individual> spouses = new ArrayList<>();
            buildSpouseGroupInto(spouses);
            for (final Individual spouse : spouses) {
                if (spouse == this || spouse.idxFather < 0) {
                    spouse.house = proot;
                    spouse.mark = true;
                }
            }
        }

        void setSeqWithSpouses(final List<Double> lev_bounds, final boolean left, final List<Individual> cleannext) {
            final LinkedList<Individual> spouses = new LinkedList<>();
            buildSpouseGroupInto(spouses);
            final Iterator<Individual> i = spouses.descendingIterator();
            while (i.hasNext()) {
                final Individual indi = i.next();
                if (indi.idxFather >= 0) {
                    final Individual parent = layout.indis.get(indi.idxFather);
                    if (parent.house != null && parent.house != house) {
                        cleannext.add(parent.house);
                    }
                }
                if (indi.idxMother >= 0) {
                    final Individual parent = layout.indis.get(indi.idxMother);
                    if (parent.house != null && parent.house != house) {
                        cleannext.add(parent.house);
                    }
                }
            }

            if (mark) {
                return;
            }

            LinkedList<Individual> left_sps = new LinkedList<>();
            // build list of spouses to be displayed off to the LEFT of the indi
            {
                left_sps.add(this);
                Individual pindi = this;
                while (pindi != null) {
                    boolean found = false;
                    for (int sp = 0; !found && sp < pindi.ridxSpouse.size(); ++sp) {
                        final Individual pspou = layout.indis.get(pindi.ridxSpouse.get(sp));
                        if (!pspou.mark && pspou.idxFather < 0 && pspou != this && !left_sps.contains(pspou)) {
                            found = true;
                            pindi = pspou;
                            left_sps.add(pspou);
                        }
                    }
                    if (!found) {
                        pindi = null;
                    }
                }
            }

            LinkedList<Individual> right_sps = new LinkedList<>();
            // build list of spouses to be displayed off to the RIGHT of the indi
            {
                Individual pindi = this;
                while (pindi != null) {
                    boolean found = false;
                    for (int sp = 0; !found && sp < pindi.ridxSpouse.size(); ++sp) {
                        final Individual pspou = layout.indis.get(pindi.ridxSpouse.get(sp));
                        if (!pspou.mark && pspou.idxFather < 0 && pspou != this && !left_sps.contains(pspou) && !right_sps.contains(pspou)) {
                            found = true;
                            pindi = pspou;
                            right_sps.add(pspou);
                        }
                    }
                    if (!found) {
                        pindi = null;
                    }
                }
            }
            //add (to the right) all remaining spouses
            for (final Individual pspou : spouses) {
                if (!pspou.mark && pspou.idxFather < 0 && !left_sps.contains(pspou) && !right_sps.contains(pspou)) {
                    right_sps.add(pspou);
                }
            }

            if (!left) {
                final LinkedList<Individual> t = left_sps;
                left_sps = right_sps;
                right_sps = t;
            }

            left_sps.descendingIterator().forEachRemaining(s -> displaySpouses(lev_bounds, s));
            right_sps.iterator().forEachRemaining(s -> displaySpouses(lev_bounds, s));
        }

        private void buildSpouseGroupInto(final Collection<Individual> spouses) {
            final LinkedList<Individual> todo = new LinkedList<>();
            if (!mark) {
                todo.addLast(this);
            }
            while (!todo.isEmpty()) {
                final Individual spouse = todo.removeFirst();
                spouses.add(spouse);
                for (int s = 0; s < spouse.ridxSpouse.size(); ++s) {
                    final Individual spouse2 = layout.indis.get(spouse.ridxSpouse.get(s));
                    if (!spouse2.mark && !spouses.contains(spouse2)) {
                        todo.addLast(spouse2);
                    }
                }
            }
        }

        private void displaySpouses(final List<Double> xForLevel, final Individual spouse) {
            spouse.moveXto(xForLevel.get(spouse.level));
            spouse.mark = true;
            xForLevel.set(spouse.level, spouse.location.getX() + 2D * MAX_WIDTH);
        }

        private long getBirthForSort() {
            return this.indi.getBirthForSort();
        }

        public void layOut() {
            this.indi.layOut(this.location);
        }
    }











    public void cleanAll() {
        // preliminary stuff
        final int cIndi = this.indis.size();
        if (cIndi <= 1) {
            return;
        }


        LOG.debug("set generation levels (also sets position on y-axis)");
        {
            clearAllIndividuals();
            int batch = 0;
            boolean someleft = true;
            while (someleft) {
                someleft = false;
                for (final Individual indi : this.indis) {
                    if (!indi.mark) {
                        someleft = true;
                        indi.setLevel(batch++ * 5);
                    }
                }
            }
        }

        LOG.debug("normalize indis' level nums");
        final int cLev; //count of levels
        {
            final int levMax = this.indis.stream().mapToInt(i -> i.level).max().getAsInt();
            final int levMin = this.indis.stream().mapToInt(i -> i.level).min().getAsInt();

            cLev = levMax - levMin + 1;
            for (final Individual indi : this.indis) {
                indi.level -= levMin;
            }
        }


        LOG.debug("calc max male-branch-descendant-generations size for all indis");
        {
            clearAllIndividuals();
            // Finding branches
            for (final Individual indi : this.indis) {
                int c = (indi.sex == 1) ? 1 : 0;

                final Set<Integer> setIndi = new HashSet<>();// guard against loops
                Individual father = indi;
                int f;
                while ((f = father.idxFather) >= 0 && !setIndi.contains(f)) {
                    setIndi.add(f);
                    ++c;
                    father = this.indis.get(f);
                }
                father.setMaxMaleIf(c);
                if (father.idxMother >= 0) {
                    this.indis.get(father.idxMother).maxMale = c + 1;
                }
            }
        }


        final Deque<Individual> qToClean =
                this.indis.stream()
                        .filter(i -> i.maxMale != 0)
                        .sorted(primaryHouse())
                        .collect(Collectors.toCollection(LinkedList::new));


        LOG.debug("Labeling branches");

        clearAllIndividuals();

        for (final Individual indi : qToClean) {
            final Deque<Individual> todo = new LinkedList<>();

            indi.setRootWithSpouses(indi);
            todo.addLast(indi);
            while (!todo.isEmpty()) {
                final Individual pgmi = todo.removeFirst();
                for (int j = 0; j < pgmi.ridxSpouseToFamily.size(); ++j) {
                    final Family fami = this.famis.get(pgmi.ridxSpouseToFamily.get(j));
                    for (int k = 0; k < fami.children.size(); ++k) {
                        final Individual pchil = this.indis.get(fami.children.get(k));
                        if (!pchil.mark) {
                            pchil.setRootWithSpouses(indi);
                            if (pchil.sex == 1) {
                                todo.addLast(pchil);
                            }
                        }
                    }
                }
            }
        }


        LOG.debug("build new list with only house heads");
        final Deque<Individual> rptoclean2 = new LinkedList<>();
        final Set<Individual> settoclean2 = new HashSet<>();
        {
            // Finding progenitors
            //make a list of all house heads
            final Set<Integer> setheads = new HashSet<>();
            for (final Individual pindi : this.indis) {
                if (pindi.house != null) {
                    setheads.add(pindi.house.idx);
                }
            }

            // put house heads on rptoclean2 list in order of processing
            for (final Individual psec : qToClean) {
                if (setheads.contains(psec.idx)) {
                    rptoclean2.add(psec);
                    settoclean2.add(psec);
                }
            }
        }


        final List<Double> xForLevel = new ArrayList<>();
        for (int i = 0; i < cLev; ++i) {
            xForLevel.add(0.0D);
        }

        clearAllIndividuals();
        LOG.debug("Moving branches");
        while (!rptoclean2.isEmpty()) {
            final Individual psec = rptoclean2.remove();
            settoclean2.remove(psec);
            LOG.debug("branch head: {}", psec.indi.name());

            final List<Individual> nexthouse = new ArrayList<>();

            final Set<Individual> guard = new HashSet<>();
            final List<Individual> todo = new ArrayList<>();
            todo.add(psec);
            guard.add(psec);
            while (!todo.isEmpty()) {
                final Individual pgmi = todo.remove(0);
                final List<Individual> cleannext = new ArrayList<>();
                pgmi.setSeqWithSpouses(xForLevel, false, cleannext);
                nexthouse.addAll(cleannext);

                for (int j = 0; j < pgmi.ridxSpouseToFamily.size(); ++j) {
                    final Family fami = this.famis.get(pgmi.ridxSpouseToFamily.get(j));
                    final List<Integer> riChild = new ArrayList<>();
                    fami.GetSortedChildren(riChild);
                    int nch = riChild.size();
                    if (nch > 0) {
                        // put the (first two) children with spouses on the outside edges
                        // search for children in "flip-flopping" order, viz.: 1, n, 2, n-1, ...
                        int sp1 = -1;
                        int sp2 = -1;
                        for (int ch = 0; ch < nch; ++ch) {
                            final int fch = flop(ch, nch);
                            final Individual chil = this.indis.get(riChild.get(fch));
                            if (!chil.ridxSpouse.isEmpty()) {
                                if (sp1 < 0) {
                                    sp1 = fch;
                                } else if (sp2 < 0) {
                                    sp2 = fch;
                                }
                            }
                        }

                        final List<Integer> riChild2 = new ArrayList<>();
                        if (sp1 >= 0) {
                            riChild2.add(riChild.get(sp1));
                        }
                        for (int ch = 0; ch < nch; ++ch) {
                            if (ch != sp1 && ch != sp2) {
                                riChild2.add(riChild.get(ch));
                            }
                        }
                        if (sp2 >= 0) {
                            riChild2.add(riChild.get(sp2));
                        }
                        nch = riChild2.size();

                        boolean left = (nch > 1);
                        for (int k = 0; k < nch; ++k) {
                            final Individual pchil = this.indis.get(riChild2.get(k));
                            final List<Individual> cleannext2 = new ArrayList<>();
                            pchil.setSeqWithSpouses(xForLevel, left, cleannext2);
                            nexthouse.addAll(cleannext2);
                            left = false;
                            if (/* TODO why was this here? it caused some children to be skipped altogether: pchil.sex == 1 &&*/ !guard.contains(pchil)) {
                                todo.add(pchil);
                                guard.add(pchil);
                            }
                        }
                    }
                }
            }

            double xMax = Double.NEGATIVE_INFINITY;
            boolean any = false;
            for (int j = 0; j < cLev; ++j) {
                if (xMax < xForLevel.get(j)) {
                    xMax = xForLevel.get(j);
                }
                //kludge to see if any people in this house
                if (j > 0 && !xForLevel.get(j).equals(xForLevel.get(j - 1))) {
                    any = true;
                }
            }
            if (any) {
                xMax += DX_FAMILY;
                for (int j = 0; j < cLev; ++j) {
                    xForLevel.set(j, xMax);
                }
            }

            for (final Individual pindi : nexthouse) {
                if (settoclean2.contains(pindi)) {
                    rptoclean2.remove(pindi);
                    rptoclean2.add(pindi);
                }
            }
        }

        this.indis.forEach(Individual::layOut);
    }

    private static int flop(final int ch, final int nch) {
        final int h = ch/2;
        return (ch == 2*h) ? h : nch-(h+1);
    }


    private static Comparator<Individual> primaryHouse() {
        return Comparator
                .comparingInt((Individual i) -> i.maxMale)
                .thenComparingInt(i -> i.level)
                .thenComparingInt(i -> i.sex)
                .reversed();
    }


    private void clearAllIndividuals() {
        this.indis.forEach(i -> i.mark = false);
    }
}
