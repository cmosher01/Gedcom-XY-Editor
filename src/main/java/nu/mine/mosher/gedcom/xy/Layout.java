package nu.mine.mosher.gedcom.xy;

import nu.mine.mosher.time.Time;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
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


public class Layout {
    private final double dxChild;
    private final double dxFamily;

    public Layout(final List<Indi> indis, final List<Fami> famis, final Metrics metrics) {
        this.dxChild = metrics.getWidthMax();
        this.dxFamily = this.dxChild * 4;

        this.indis = IntStream.range(0, indis.size())
                .mapToObj(i -> new CIndividual(indis.get(i), i))
                .collect(Collectors.toList());

        final Map<Indi, CIndividual> mapIndis = new HashMap<>();
        this.indis.forEach(i -> mapIndis.put(i.indi, i));

        this.famis = IntStream.range(0, famis.size())
                .mapToObj(i -> new CFamily(famis.get(i), i))
                .collect(Collectors.toList());

        final Map<Fami, CFamily> mapFamis = new HashMap<>();
        this.famis.forEach(i -> mapFamis.put(i.fami, i));

        this.famis.forEach(f -> {
            final Fami fami = f.fami;

            final Optional<Indi> husb = fami.getHusb();
            final Optional<Indi> wife = fami.getWife();

            addSpouse(mapIndis, f, husb, wife);
            addSpouse(mapIndis, f, wife, husb);

            fami.getChildren().forEach(c -> {
                final CIndividual cChild = mapIndis.get(c);
                cChild.idxChildToFamily = f.idx;
                f.children.add(cChild.idx);
                if (husb.isPresent()) {
                    final CIndividual cHusb = mapIndis.get(husb.get());
                    cHusb.ridxChild.add(mapIndis.get(c).idx);
                    cChild.idxFather = cHusb.idx;
                }
                if (wife.isPresent()) {
                    final CIndividual cWife = mapIndis.get(wife.get());
                    cWife.ridxChild.add(mapIndis.get(c).idx);
                    cChild.idxMother = cWife.idx;
                }
            });
        });
    }

    private static void addSpouse(Map<Indi, CIndividual> mapIndis, CFamily fami, Optional<Indi> indi, Optional<Indi> spouse) {
        if (indi.isPresent()) {
            final CIndividual cindi = mapIndis.get(indi.get());
            cindi.ridxSpouseToFamily.add(fami.idx);
            spouse.ifPresent(i -> cindi.ridxSpouse.add(mapIndis.get(i).idx));
        }
    }


    private final List<CIndividual> indis;
    private final List<CFamily> famis;


    class CFamily {
        private final int idx;
        private final Fami fami;

        /* indexes into indis list */
        private final List<Integer> children = new ArrayList<>();

        public CFamily(final Fami fami, final int idx) {
            this.fami = fami;
            this.idx = idx;
        }

        void GetSortedChildren(final List<Integer> riChild) {
            riChild.clear();
            riChild.addAll(children);
            riChild.sort(Comparator.comparing(i -> Layout.this.indis.get(i).getSimpleBirth()));
        }
    }


    class CIndividual {
        private Layout m_pDoc = Layout.this;

        private int sex; // 0=unknown, 1=male, 2=female
        private Rectangle2D frame;

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
        private CIndividual house;


        public CIndividual(final Indi indi, final int idx) {
            this.indi = indi;
            this.idx = idx;
            this.sex = indi.getSex();
            this.frame = new Rectangle2D.Double(indi.getCircle().getCenterX(), indi.getCircle().getCenterY(), 0D, 0D);
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
            MoveTo(new Point2D.Double(0.0D, (5000.0D - this.level) * 100.0D - this.frame.getHeight() / 2.0D));

            //father
            if (idxFather >= 0) {
                m_pDoc.indis.get(idxFather).setLevel(lev + 1);
            }
            //mother (only if no father)
            else if (idxMother >= 0) {
                m_pDoc.indis.get(idxMother).setLevel(lev + 1);
            }

            //siblings
            if (idxChildToFamily >= 0) {
                final CFamily fami = m_pDoc.famis.get(idxChildToFamily);
                for (int i = 0; i < fami.children.size(); ++i) {
                    m_pDoc.indis.get(fami.children.get(i)).setLevel(lev);
                }
            }

            //children
            for (final Integer i : ridxChild) {
                m_pDoc.indis.get(i).setLevel(lev - 1);
            }

            //spouses
            for (final Integer i : ridxSpouse) {
                m_pDoc.indis.get(i).setLevel(lev);
            }
        }

        void MoveTo(final Point2D pt) {
            this.frame.setRect(pt.getX(), pt.getY(), this.frame.getWidth(), this.frame.getHeight());
        }

        void setMaxMaleIf(final int n) {
            if (maxMale < n) {
                maxMale = n;
            }
        }

        void setRootWithSpouses(final CIndividual proot) {
            if (mark) {
                return;
            }

            final List<CIndividual> all_sps = new ArrayList<>();
            // build list of all spouses in this spouse group
            {
                final LinkedList<CIndividual> todo = new LinkedList<>();
                todo.addLast(this);
                while (!todo.isEmpty()) {
                    final CIndividual pthis = todo.removeFirst();
                    all_sps.add(pthis);
                    for (int sp = 0; sp < pthis.ridxSpouse.size(); ++sp) {
                        final CIndividual pspou = m_pDoc.indis.get(pthis.ridxSpouse.get(sp));
                        if (/*pspou->level == level &&*/!pspou.mark && !all_sps.contains(pspou)) {
                            todo.addLast(pspou);
                        }
                    }
                }
            }

            for (final CIndividual pindi2 : all_sps) {
                if (pindi2 == this || pindi2.idxFather < 0) {
                    pindi2.house = proot;
                    pindi2.mark = true;
                }
            }
        }

        void setSeqWithSpouses(final List<Double> lev_bounds, final boolean left, final List<CIndividual> cleannext) {
            final LinkedList<CIndividual> all_sps = new LinkedList<>();
            // build list of all spouses in this spouse group
            {
                final LinkedList<CIndividual> todo = new LinkedList<>();
                if (!mark) {
                    todo.addLast(this);
                }
                while (!todo.isEmpty()) {
                    final CIndividual pthis = todo.removeFirst();
                    all_sps.add(pthis);
                    for (int sp = 0; sp < pthis.ridxSpouse.size(); ++sp) {
                        final CIndividual pspou = m_pDoc.indis.get(pthis.ridxSpouse.get(sp));
                        if (/*pspou->level == level &&*/!pspou.mark && !all_sps.contains(pspou)) {
                            todo.addLast(pspou);
                        }
                    }
                }
            }

            final Iterator<CIndividual> i = all_sps.descendingIterator();
            while (i.hasNext()) {
                final CIndividual indi = i.next();
                if (indi.idxFather >= 0) {
                    final CIndividual parent = m_pDoc.indis.get(indi.idxFather);
                    if (parent.house != null && parent.house != house) {
                        cleannext.add(parent.house);
                    }
                }
                if (indi.idxMother >= 0) {
                    final CIndividual parent = m_pDoc.indis.get(indi.idxMother);
                    if (parent.house != null && parent.house != house) {
                        cleannext.add(parent.house);
                    }
                }
            }

            if (mark) {
                return;
            }

            LinkedList<CIndividual> left_sps = new LinkedList<>();
            // build list of spouses to be displayed off to the LEFT of the indi
            {
                left_sps.add(this);
                CIndividual pindi = this;
                while (pindi != null) {
                    boolean found = false;
                    for (int sp = 0; !found && sp < pindi.ridxSpouse.size(); ++sp) {
                        final CIndividual pspou = m_pDoc.indis.get(pindi.ridxSpouse.get(sp));
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

            LinkedList<CIndividual> right_sps = new LinkedList<>();
            // build list of spouses to be displayed off to the RIGHT of the indi
            {
                CIndividual pindi = this;
                while (pindi != null) {
                    boolean found = false;
                    for (int sp = 0; !found && sp < pindi.ridxSpouse.size(); ++sp) {
                        final CIndividual pspou = m_pDoc.indis.get(pindi.ridxSpouse.get(sp));
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
            for (final CIndividual pspou : all_sps) {
                if (!pspou.mark && pspou.idxFather < 0 && !left_sps.contains(pspou) && !right_sps.contains(pspou)) {
                    right_sps.add(pspou);
                }
            }

            if (!left) {
                final LinkedList<CIndividual> t = left_sps;
                left_sps = right_sps;
                right_sps = t;
            }

            left_sps.descendingIterator().forEachRemaining(s -> displaySpouses(lev_bounds, s));
            right_sps.iterator().forEachRemaining(s -> displaySpouses(lev_bounds, s));
        }

        private void displaySpouses(final List<Double> lev_bounds, final CIndividual pindi) {
            pindi.MoveTo(new Point2D.Double(lev_bounds.get(pindi.level), pindi.frame.getY()));
            pindi.mark = true;
            lev_bounds.set(pindi.level, pindi.frame.getMaxX() + dxChild);
        }

        private Time getSimpleBirth() {
            return this.indi.getBirth().getStartDate().getApproxDay();
        }
    }


    public void cleanAll() {
        // preliminary stuff
        final int cIndi = this.indis.size();
        if (cIndi <= 1) {
            return;
        }


        // set generation levels (also sets position on y-axis)
        {
            clearAllIndividuals();
            int batch = 0;
            boolean someleft = true;
            while (someleft) {
                someleft = false;
                for (final CIndividual indi : this.indis) {
                    if (!indi.mark) {
                        someleft = true;
                        indi.setLevel(batch++ * 20);
                    }
                }
            }
        }

        // normalize indis' level nums
        final int cLev; //count of levels
        {
            final int levMax = this.indis.stream().mapToInt(i -> i.level).max().getAsInt();
            final int levMin = this.indis.stream().mapToInt(i -> i.level).min().getAsInt();

            cLev = levMax - levMin + 1;
            for (final CIndividual indi : this.indis) {
                indi.level -= levMin;
            }
        }


        // calc max male-branch-descendant-generations size for all indis
        {
            clearAllIndividuals();
            // Finding branches
            for (final CIndividual indi : this.indis) {
                int c = (indi.sex == 1) ? 1 : 0;

                final Set<Integer> setIndi = new HashSet<>();// guard against loops
                CIndividual father = indi;
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


        final Deque<CIndividual> qToClean =
                this.indis.stream()
                        .filter(i -> i.maxMale != 0)
                        .sorted(primaryHouse())
                        .collect(Collectors.toCollection(LinkedList::new));


        // Labeling branches

        clearAllIndividuals();

        for (final CIndividual indi : qToClean) {
            final Deque<CIndividual> todo = new LinkedList<>();

            indi.setRootWithSpouses(indi);
            todo.addLast(indi);
            while (!todo.isEmpty()) {
                final CIndividual pgmi = todo.removeFirst();
                for (int j = 0; j < pgmi.ridxSpouseToFamily.size(); ++j) {
                    final CFamily fami = this.famis.get(pgmi.ridxSpouseToFamily.get(j));
                    for (int k = 0; k < fami.children.size(); ++k) {
                        final CIndividual pchil = this.indis.get(fami.children.get(k));
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


        // build new list with only house heads
        final Deque<CIndividual> rptoclean2 = new LinkedList<>();
        final Set<CIndividual> settoclean2 = new HashSet<>();
        {
            // Finding progenitors
            //make a list of all house heads
            final Set<Integer> setheads = new HashSet<>();
            for (final CIndividual pindi : this.indis) {
                if (pindi.house != null) {
                    setheads.add(pindi.house.idx);
                }
            }

            // put house heads on rptoclean2 list in order of processing
            for (final CIndividual psec : qToClean) {
                if (setheads.contains(psec.idx)) {
                    rptoclean2.add(psec);
                    settoclean2.add(psec);
                }
            }
        }


        final List<Double> lev_bounds = new ArrayList<>();
        for (int i = 0; i < cLev; ++i) {
            lev_bounds.add(0.0D);
        }

        clearAllIndividuals();
        // Moving branches.
        while (!rptoclean2.isEmpty()) {
            final CIndividual psec = rptoclean2.remove();
            settoclean2.remove(psec);

            final List<CIndividual> nexthouse = new ArrayList<>();

            final Set<CIndividual> guard = new HashSet<>();
            final List<CIndividual> todo = new ArrayList<>();
            todo.add(psec);
            guard.add(psec);
            while (!todo.isEmpty()) {
                final CIndividual pgmi = todo.remove(0);
                final List<CIndividual> cleannext = new ArrayList<>();
                pgmi.setSeqWithSpouses(lev_bounds, false, cleannext);
                nexthouse.addAll(cleannext);

                for (int j = 0; j < pgmi.ridxSpouseToFamily.size(); ++j) {
                    final CFamily fami = this.famis.get(pgmi.ridxSpouseToFamily.get(j));
                    final List<Integer> riChild = new ArrayList<>();
                    fami.GetSortedChildren(riChild);
                    int nch = riChild.size();
                    if (nch > 0) {
                        //put the (first two) children with spouses on the outside edges
                        int sp1 = -1;
                        int sp2 = -1;
                        for (int ch = 0; ch < nch; ++ch) {
                            final CIndividual chil = this.indis.get(riChild.get(ch));
                            if (!chil.ridxSpouse.isEmpty()) {
                                if (sp1 < 0) {
                                    sp1 = ch;
                                } else if (sp2 < 0) {
                                    sp2 = ch;
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
                            final CIndividual pchil = this.indis.get(riChild2.get(k));
                            final List<CIndividual> cleannext2 = new ArrayList<>();
                            pchil.setSeqWithSpouses(lev_bounds, left, cleannext2);
                            nexthouse.addAll(cleannext2);
                            lev_bounds.set(pchil.level, lev_bounds.get(pchil.level) + dxChild);
                            left = false;
                            if (pchil.sex == 1 && !guard.contains(pchil)) {
                                todo.add(pchil);
                                guard.add(pchil);
                            }
                        }
                    }
                }
            }

            double maxx = Double.NEGATIVE_INFINITY;
            boolean any = false;
            for (int j = 0; j < cLev; ++j) {
                if (maxx < lev_bounds.get(j)) {
                    maxx = lev_bounds.get(j);
                }
                //kludge to see if any people in this house
                if (j > 0 && !lev_bounds.get(j).equals(lev_bounds.get(j - 1))) {
                    any = true;
                }
            }
            if (any) {
                maxx += dxFamily;
                for (int j = 0; j < cLev; ++j) {
                    lev_bounds.set(j, maxx);
                }
            }

            for (final CIndividual pindi : nexthouse) {
                if (settoclean2.contains(pindi)) {
                    rptoclean2.remove(pindi);
                    rptoclean2.add(pindi);
                }
            }
        }

        this.indis.forEach(i -> i.indi.setOrigCoords(i.frame.getX(), i.frame.getY()));
    }


    private static Comparator<CIndividual> primaryHouse() {
        return Comparator
                .comparingInt((CIndividual i) -> i.maxMale)
                .thenComparingInt(i -> i.level)
                .thenComparingInt(i -> i.sex)
                .reversed();
    }


    private void clearAllIndividuals() {
        this.indis.forEach(i -> i.mark = false);
    }
}
