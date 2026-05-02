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
import nu.mine.mosher.gedcom.xy.shape.Translation;
import org.slf4j.*;

import java.util.*;
import java.util.stream.*;

/**
 * Genealogical automatic intelligent drop-line chart layout algorithm.
 */
public class Layout {
    private static final Logger LOG = LoggerFactory.getLogger(Layout.class);

    public static final double MAX_LEVEL = 5000.0D;
    private static final double GEN_HEIGHT = 108D;
    private static final double MAX_WIDTH = 54D; // TODO calculate max width?
    private static final double DX_INDIVIDUAL = 1.2D * MAX_WIDTH;
    public static final Translation RIGHT_OF = new Translation(DX_INDIVIDUAL, 0.0D);
    private static final double DY_INDIVIDUAL = GEN_HEIGHT; // TODO is this good?
    public static final Translation BELOW = new Translation(0.0D, DY_INDIVIDUAL);
    public static final Translation ABOVE = new Translation(0.0D, -DY_INDIVIDUAL);
    public static final Translation GEN_BELOW = new Translation(0.0D, GEN_HEIGHT);
    public static final double DX_FAMILY = 5.0D * MAX_WIDTH;

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

            f.husb = spidx(mapIndis, husb);
            f.wife = spidx(mapIndis, wife);

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
                    //f.husb = cHusb.idx; // TODO test this
                }
                if (wife.isPresent()) {
                    final Individual cWife = mapIndis.get(wife.get());
                    cWife.ridxChild.add(mapIndis.get(c).idx);
                    cChild.idxMother = cWife.idx;
                    //f.wife = cWife.idx; // TODO test this
                }
            });
        });
    }

    private static int spidx(final Map<Indi, Individual> mapIndis, final Optional<Indi> spouse) {
        if (spouse.isEmpty()) {
            return -1;
        }
        final var optIdv = Optional.ofNullable(mapIndis.get(spouse.get()));
        if (optIdv.isEmpty()) {
            return -1;
        }
        return optIdv.get().idx;
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
        private int husb = -1;
        private int wife = -1;
        private final List<Integer> children = new ArrayList<>();

        public Family(final Fami fami, final int idx) {
            this.fami = fami;
            this.idx = idx;
        }

        void getSortedChildren(final List<Integer> riChild) {
            riChild.clear();
            riChild.addAll(children);
            riChild.sort(Comparator.comparing(i -> Layout.this.indis.get(i).getBirthForSort()));
        }

//        public boolean isUnplacedFully() {
//            final var unplacedHusb = isUnplacedIdvOpt(this.husb);
//            final var unplacedWife = isUnplacedIdvOpt(this.wife);
//
//            return unplacedHusb && unplacedWife && isUnplacedAllChildren();
//        }

//        public Optional<Individual> findExactlyOneParentUnplaced() {
//            final var unplacedHusb = isUnplacedIdvOpt(this.husb);
//            final var unplacedWife = isUnplacedIdvOpt(this.wife);
//            final var unplacedExactlyOneParent =
//                (!unplacedHusb &&  unplacedWife) ||
//                ( unplacedHusb && !unplacedWife);
//
//            final Optional<Individual> ret;
//            if (unplacedExactlyOneParent && isUnplacedAllChildren()) {
//                if (unplacedHusb) {
//                    ret = Optional.of(Layout.this.indis.get(husb));
//                } else {
//                    ret = Optional.of(Layout.this.indis.get(wife));
//                }
//            } else {
//                ret = Optional.empty();
//            }
//
//            return ret;
//        }

//        private boolean isUnplacedAllChildren() {
//            int cChildPlaced = 0;
//            for (final var iChild : this.children) {
//                final var isPlacedChild = isUnplacedIdvOpt(iChild);
//                if (isPlacedChild) {
//                    cChildPlaced++;
//                }
//            }
//            return (cChildPlaced == 0);
//        }

//        private boolean isUnplacedIdvOpt(int iIdv) {
//            final var ivdOpt = Layout.this.indis.get(iIdv);
//            return Objects.nonNull(ivdOpt) && ivdOpt.isPlaced();
//        }

//        public Individual getOneParent() {
//            final var idvHusbOpt = Layout.this.indis.get(this.husb);
//            if (Objects.nonNull(idvHusbOpt)) {
//                return idvHusbOpt;
//            }
//            final var idvWifeOpt = Layout.this.indis.get(this.wife);
//            if (Objects.nonNull(idvWifeOpt)) {
//                return idvWifeOpt;
//            }
//            return null;
//        }





// used to classify families into use cases concerning placed vs. unplaced parents and children

        public boolean hasPlacedChildren() {
            return (0 < cPlacedChildren());
        }

        /**
         * count of placed children
         * @return 0 if no children, otherwise count of children that are placed
         */
        public int cPlacedChildren() {
            return (int)this.children.stream().filter(Layout.this::isPlacedIdv).count();
        }

        /**
         * count of parents
         * @return 0, 1, or 2
         */
        public int cParents() {
            int c = 0;
            if (findIdvByIdx(this.husb).isPresent()) {
                c++;
            }
            if (findIdvByIdx(this.wife).isPresent()) {
                c++;
            }
            return c;
        }

        /**
         * Count of placed (existing) parents
         * If zero parents exist, returns 0;
         * If exactly one parent exists, returns 0 if it is unplaced, 1 if it is placed.
         * If both parents exist, returns how many of them are placed (0, 1, or 2).
         * @return 0, 1, or 2
         */
        public int cPlacedParents() {
            int c = 0;
            if (isPlacedIdv(this.husb)) {
                c++;
            }
            if (isPlacedIdv(this.wife)) {
                c++;
            }
            return c;
        }
    }








    class Individual {
        private final Layout layout = Layout.this;

        private final int sex; // 0=unknown, 1=male, 2=female
        private Point2D location = Point2D.ZERO;

        private final Indi indi;
        /* indexes into indis list */
        private final int idx;
        private int idxFather = -1;
        private int idxMother = -1;
        private final List<Integer> ridxSpouse = new ArrayList<>();
        private final List<Integer> ridxChild = new ArrayList<>();
        /* indexes into famis list */
        private int idxChildToFamily = -1;
        private final List<Integer> ridxSpouseToFamily = new ArrayList<>();

        private boolean mark;
        private int level;
        private int maxMale;
        private Individual house;


        public Individual(final Indi indi, final int idx) {
            this.indi = indi;
            this.idx = idx;
            this.sex = indi.getSex();
        }

        public Indi getOrigIndi() {
            return this.indi;
        }

        void setLevel(final int lev) {
            this.level = lev;

            // position along y axis
            moveYto((MAX_LEVEL - this.level) * GEN_HEIGHT);
        }

        void addRelativesTo(final Deque<LevelSetting> levelSettingDeque) {
            final int lev = this.level;

            //father
            if (idxFather >= 0) {
                levelSettingDeque.offer(new LevelSetting(layout.indis.get(idxFather), lev + 1));
            }
            //mother (only if no father)
            else if (idxMother >= 0) {
                levelSettingDeque.offer(new LevelSetting(layout.indis.get(idxMother), lev + 1));
            }

            //siblings
            if (idxChildToFamily >= 0) {
                final Family fami = layout.famis.get(idxChildToFamily);
                for (int i = 0; i < fami.children.size(); ++i) {
                    levelSettingDeque.offer(new LevelSetting(layout.indis.get(fami.children.get(i)), lev));
                }
            }

            //children
            for (final Integer i : ridxChild) {
                levelSettingDeque.offer(new LevelSetting(layout.indis.get(i), lev - 1));
            }

            //spouses
            for (final Integer i : ridxSpouse) {
                levelSettingDeque.offer(new LevelSetting(layout.indis.get(i), lev));
            }
        }

        void moveToAndLayOut(final Point2D to) {
            this.location = to;
            layOut();
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

        public boolean isPlaced() {
            return !getLocationOrOriginal().equals(Point2D.ZERO);
        }

        public Point2D getLocationOrOriginal() {
            final Point2D ret;
            if (this.location.equals(Point2D.ZERO)) {
                ret = this.indi.coordsOriginal().orElse(Point2D.ZERO);
            } else {
                ret = this.location;
            }
            return ret;
        }
    }


    class LevelSetting {
        public final Individual indi;
        public final int level;

        public LevelSetting(final Individual indi, final int level) {
            this.indi = indi;
            this.level = level;
        }
    }





    private void setIslandLevels(final Individual indi, final int level) {
        final Deque<LevelSetting> levelSettingDeque = new LinkedList<>();
        levelSettingDeque.offer(new LevelSetting(indi,level));

        while (!levelSettingDeque.isEmpty()) {
            final LevelSetting setting = levelSettingDeque.poll();
            if (!setting.indi.mark) {
                setting.indi.mark = true;
                setting.indi.setLevel(setting.level);
                setting.indi.addRelativesTo(levelSettingDeque);
            }
        }
    }











    public void clean() {
        if (this.indis.size() <= 1) {
            return;
        }

        if (this.indis.stream().noneMatch(i -> i.getOrigIndi().hadOriginalXY())) {
            cleanAll();
        } else {
            cleanUnplaced();
        }
    }








// PARTIAL LAYOUT



    /*
        Prime directive: Do not move any people that already have coordinates
        in the original file.

        Strategy: Go through each family (father, mother, children) in the tree
        and check the status of each person in the family to see which ones have
        no _XY value (i.e., are "unplaced"), and therefore need to be laid out.

        Case: Entire family needs to be laid out. Lay them out as expected,
        with father and mother above and centered around the children in a row.
        These families will be placed BELOW the existing tree, each one under
        the previous (for easy group selection by the end user).

        Case: Exactly one parent is laid out. Perform same layout as the previous
        case, but place everyone relative to the existing laid out person.

        Case: Both parents are laid out. Perform same layout as above (but relative
        to both parents).

        Case: One or more children are laid out. Lay out unplaced children to the
        right of the right-most existing child.

        These layouts may or may not be cascading. For example, we lay out one
        entire family and place it below the existing tree; then, if one of THOSE
        children happens to be the only parent in another family than needs to
        be laid out, that other family will be place relative to the first family.
        It makes a difference which family we lay out first, but for (now we) don't
        do any smart checking to see which family should be laid out first.

        TODO: When moving (any, or specific) people, should we prevent overlapping
        with other people in the tree?
     */

    /*
        Different cases of the parents being placed within a family:
        1.  No parents in family.
        2.  Exactly one parent in family:
            a.  Unplaced.
            b.  Placed.
        3.  Two parents in family:
            a.  Neither placed.
            b.  Exactly one placed.
            c.  Both placed.

        Different cases of the children being placed within a family:
        U.  No children, or only unplaced children, in family.
        P.  At least one placed child within family, and either no other children,
            or only unplaced other children, in family.

        How to handle each combination. After each case's action is complete, it could
        possibly degenerate into another case (noted below), or not (indicated as "done"):
        U:
            1 . Place first child (if any) below bottom of tree.
                [next: P1.]
            2a. Place parent below bottom of tree.
                [next: U2b.]
            2b. Place first child (if any) below parent.
                [next: P2b.]
            3a. Place husband below bottom of tree (or next to spouse, if any other???)
                [next: U3b.]
            3b. Place unplaced parent to right of placed parent.
                [next: U3c.]
            3c. Place first child (if any) (at left-most parent x, below bottom-most parent y)
                [next: P3c.]
        P:
            1 . Place any and all unplaced other children to right of rightmost placed child.
                [done]
            2a. Place parent (at leftmost placed child x, above topmost placed child y).
                [next: P2b.]
            2b. Same as 1.
                [done]
            3a. Place husband (at leftmost placed child x, above topmost placed child y).
                [next: P3b.]
            3b. Place unplaced parent to right of placed parent.
                [next: P3c.]
            3c. Same as 1.
                [done]
    */


//    private void placeUnplacedFamily(final Family fml) {
//        // If fully unplaced, place one parent below existing tree
//        if (fml.isUnplacedFully()) {
//            final var idvParent = fml.getOneParent();
//            if (Objects.isNull(idvParent)) {
//                // TODO fully unplaced children with no parents
//            } else {
//                placeIdvBelowOther(idvParent, getLowest());
//            }
//        }
//
//        // If exactly one parent unplaced, place other parent to right of already placed parent
//        final var optOnlyPlacedParent = fml.findExactlyOneParentUnplaced();
//        if (optOnlyPlacedParent.isPresent()) {
//            placeIdvToRightOfRightmostPlacedSpouse(optOnlyPlacedParent.get());
//            // TODO if no spouse exists, ^^^ this does nothing
//        } else {
//            // TODO either no other spouse exists, or
//        }
//    }


//    private void cleanUnplaced() {
//        final var todo = new LinkedList<>(
//            this.indis.stream()
//                .filter(i -> !i.getOrigIndi().hadOriginalXY())
//                .toList());
//
//        final var done = new HashSet<Individual>();
//
//        todo.forEach(i -> cleanUnplacedWithSpouse(i, done));
//        todo.removeAll(done);
//        done.clear();
//
//        todo.forEach(i -> cleanUnplacedSiblings(i, done));
//        todo.removeAll(done);
//        done.clear();
//
//        // TODO more cleaning
//    }
//
//    private void cleanUnplacedSiblings(final Individual idv, final HashSet<Individual> done) {
//        if (done.contains(idv)) {
//            return;
//        }
//
//        if (0 <= idv.idxChildToFamily) {
//            final Family fml = famis.get(idv.idxChildToFamily);
//            if (2 <= fml.children.size()) {
//                fml.children.forEach(c -> indis.get(c).);
//                done.add(idv);
//            }
//        }
//    }
//
//    private void cleanUnplacedWithSpouse(final Individual idv, final HashSet<Individual> done) {
//        if (!idv.ridxSpouse.isEmpty()) {
//            final var sp = indis.get(idv.ridxSpouse.get(0));
//            if (sp.isPlaced()) {
//                placeRelativeTo(idv, sp, new Translation(DX_INDIVIDUAL, 0.0D));
//                done.add(idv);
//            }
//        }
//    }

    public void cleanUnplaced() {
        this.famis.forEach(this::cleanFamily);
    }

    private void cleanFamily(final Family fml) {
        // WARNING: values within each if block can modify the
        // results of future if statement conditions. Don't
        // rearrange or refactor without careful testing.
        if (!fml.hasPlacedChildren() && fml.cParents() == 0) {
            // U1 . No parents in family.
            //      No children, or only unplaced children, in family.
            //
            //      Place first child (if any) below bottom of tree.
            placeFirstChildOfFamilyAtBottom(fml);
            //     [next: P1.]
        }
        if (!fml.hasPlacedChildren() && fml.cParents() == 1 && fml.cPlacedParents() == 0) {
            // U2a. Exactly one parent in family, who is unplaced.
            //      No children, or only unplaced children, in family.
            //
            //      Place parent below bottom of tree.
            placeParentAtBottom(fml);
            //      [next: U2b.]
        }
        if (!fml.hasPlacedChildren() && fml.cParents() == 1 && fml.cPlacedParents() == 1) {
            // U2b. Exactly one parent in family, who is placed.
            //      No children, or only unplaced children, in family.
            //
            //      Place first child (if any) below parent.
            placeFirstUnplacedChildOfFamilyBelowParents(fml);
            //      [next: P2b.]
        }
        if (!fml.hasPlacedChildren() && fml.cParents() == 2 && fml.cPlacedParents() == 0) {
            // U3a. Two parents in family, neither placed.
            //      No children, or only unplaced children, in family.
            //
            //      Place husband below bottom of tree (or next to spouse, if any other???)
            placeParentAtBottom(fml);
            //      [next: U3b.]
        }
        if (!fml.hasPlacedChildren() && fml.cParents() == 2 && fml.cPlacedParents() == 1) {
            // U3b. Two parents in family, exactly one placed.
            //      No children, or only unplaced children, in family.
            //
            //      Place unplaced parent to right of placed parent.
            placeUnplacedSpouseToRightOfPlacedSpouse(fml);
            //      [next: U3c.]
        }
        if (!fml.hasPlacedChildren() && fml.cParents() == 2 && fml.cPlacedParents() == 2) {
            // U3c. Two parents in family, both placed.
            //      No children, or only unplaced children, in family.
            //
            //      Place first child (if any) (at left-most parent x, below bottom-most parent y)
            placeFirstUnplacedChildOfFamilyBelowParents(fml);
            //      [next: P3c.]
        }
        if (fml.hasPlacedChildren() && fml.cParents() == 0) {
            // P1 . No parents in family.
            //      At least one placed child within family
            //
            //      Place any and all unplaced other children to right of rightmost placed child.
            placeSubsequentUnplacedChildrenOfFamily(fml);
            //      [done]
        }
        if (fml.hasPlacedChildren() && fml.cParents() == 1 && fml.cPlacedParents() == 0) {
            // P2a. Exactly one parent in family, who is unplaced.
            //      At least one placed child within family
            //
            //      Place parent (at leftmost placed child x, above topmost placed child y).
            placeParentAboveChildren(fml);
            //      [next: P2b.]
        }
        if (fml.hasPlacedChildren() && fml.cParents() == 1 && fml.cPlacedParents() == 1) {
            // P2b. Exactly one parent in family, who is placed.
            //      At least one placed child within family
            //
            //      Place any and all unplaced other children to right of rightmost placed child.
            placeSubsequentUnplacedChildrenOfFamily(fml);
            //      [done]
        }
        if (fml.hasPlacedChildren() && fml.cParents() == 2 && fml.cPlacedParents() == 0) {
            // P3a. Two parents in family, neither placed.
            //      At least one placed child within family
            //
            //      Place husband (at leftmost placed child x, above topmost placed child y).
            placeParentAboveChildren(fml);
            //      [next: P3b.]
        }
        if (fml.hasPlacedChildren() && fml.cParents() == 2 && fml.cPlacedParents() == 1) {
            // P3b. Two parents in family, exactly one placed.
            //      At least one placed child within family
            //
            //      Place unplaced parent to right of placed parent.
            placeUnplacedSpouseToRightOfPlacedSpouse(fml);
            //      [next: P3c.]
        }
        if (fml.hasPlacedChildren() && fml.cParents() == 2 && fml.cPlacedParents() == 2) {
            // P3c. Two parents in family, both placed.
            //      At least one placed child within family
            //
            //      Place any and all unplaced other children to right of rightmost placed child.
            placeSubsequentUnplacedChildrenOfFamily(fml);
            //      [done]
        }
    }

    // if there are no placed children, does nothing
    private void placeParentAboveChildren(final Family fml) {
        final List<Integer> riChild = new ArrayList<>();
        fml.getSortedChildren(riChild);
        final var optLeftmost = findLeftmost(riChild);
        final var optTopmost = findTopmost(riChild);
        if (optLeftmost.isPresent() && optTopmost.isPresent()) {
            final Optional<Individual> optParent = findOneParent(fml);
            if (optParent.isPresent()) {
                final var topleft = new Point2D(
                    optLeftmost.get().getLocationOrOriginal().getX(),
                    optTopmost.get().getLocationOrOriginal().getY());
                optParent.get().moveToAndLayOut(ABOVE.applyTo(topleft));
            }
        }
    }


    private void placeParentAtBottom(final Family fml) {
        final Optional<Individual> optParent = findOneParent(fml);
        if (optParent.isPresent()) {
            final var posBottomLeft = getBottomLeftPositionForParent();
            optParent.get().moveToAndLayOut(posBottomLeft);
        }
    }

    private Optional<Individual> findOneParent(final Family fml) {
        final var optHusb = findIdvByIdx(fml.husb);
        final var optWife = findIdvByIdx(fml.wife);

        final Optional<Individual> optParent;
        if (optHusb.isPresent() && optWife.isPresent()) {
            optParent = optHusb;
        } else if (optHusb.isPresent()) {
            optParent = optHusb;
        } else if (optWife.isPresent()) {
            optParent = optWife;
        } else {
            optParent = Optional.empty();
        }
        return optParent;
    }

    // assume there are no placed children (i.e., no children, or only unplaced children)
    private void placeFirstChildOfFamilyAtBottom(final Family fml) {
        // Get first child in fml.
        final List<Integer> riChild = new ArrayList<>();
        fml.getSortedChildren(riChild);
        if (riChild.isEmpty()) {
            return;
        }

        final var firstChild = this.indis.get(riChild.get(0));
        final var posBottomLeft = getBottomLeftPositionForChild();
        firstChild.moveToAndLayOut(posBottomLeft);
    }

//    private void placeChildrenOfFamily(final Family fml) {
//        if (fml.children.isEmpty()) {
//            return;
//        }
//
//        placeFirstUnplacedChildOfFamily(fml);
//        placeSubsequentUnplacedChildrenOfFamily(fml);
//    }

    // U2b. Exactly one parent in family, who is placed.
    //      No children, or only unplaced children, in family.
    //
    //      Place first child (if any) below parent.
    // U3c. Two parents in family, both placed.
    //      No children, or only unplaced children, in family.
    //
    //      Place first child (if any) (at left-most parent x, below bottom-most parent y)
    private void placeFirstUnplacedChildOfFamilyBelowParents(final Family fml) {
        // Get leftmost parent
        final Optional<Individual> optLeftmostParent = findLeftmost(List.of(fml.husb, fml.wife));
        if (optLeftmostParent.isEmpty()) {
            // should never happen
            return;
        }
        // Get bottommost parent
        final Optional<Individual> optBottomParent = findBottommost(List.of(fml.husb, fml.wife));
        if (optBottomParent.isEmpty()) {
            // should never happen
            return;
        }

        // Get first child in fml.
        final List<Integer> riChild = new ArrayList<>();
        fml.getSortedChildren(riChild);
        if (riChild.isEmpty()) {
            // should never happen
            return;
        }
        final var optFirstChild = findIdvByIdx(riChild.get(0));
        if (optFirstChild.isEmpty()) {
            // should never happen
            return;
        }

        // Move first child to: (at left-most parent x, below bottom-most parent y)
        final var bottomleft = new Point2D(
            optLeftmostParent.get().getLocationOrOriginal().getX(),
            optBottomParent.get().getLocationOrOriginal().getY());

        optFirstChild.get().moveToAndLayOut(BELOW.applyTo(bottomleft));
    }

    private void placeSubsequentUnplacedChildrenOfFamily(final Family fml) {
        final var optRight = findRightmostPlacedChildOf(fml);
        if (optRight.isEmpty()) { // probably shouldn't ever happen
            return;
        }

        final List<Integer> riChild = new ArrayList<>();
        fml.getSortedChildren(riChild);

        Individual prevChild = optRight.get();
        for (final int iChild : riChild) {
            final var opt = findIdvByIdx(iChild);
            if (opt.isPresent()) {
                final var currChild = opt.get();
                placeIdvToRightOfOther(currChild, prevChild);
                prevChild = currChild;
            }
        }
    }

    private void placeIdvToRightOfRightmostPlacedSpouse(final Individual idv) {
        final var opt = findRightmostPlacedSpouseOf(idv);
        opt.ifPresent(other -> placeIdvToRightOfOther(idv, other));
    }

    private Optional<Individual> findRightmostPlacedSpouseOf(final Individual idv) {
        return findRightmost(idv.ridxSpouse);
    }

    private void placeUnplacedSpouseToRightOfPlacedSpouse(final Family fml) {
        if (isPlacedIdv(fml.husb)) {
            placeIdvToRightOfOther(findIdvByIdx(fml.wife).get(), findIdvByIdx(fml.husb).get());
        } else {
            placeIdvToRightOfOther(findIdvByIdx(fml.husb).get(), findIdvByIdx(fml.wife).get());
        }
    }

    private Optional<Individual> findRightmostPlacedSpouseOf(final Family fml) {
        final Optional<Individual> ret;

        if (isPlacedIdv(fml.husb) && isPlacedIdv(fml.wife)) {
            final var optHusb = findIdvByIdx(fml.husb);
            final var xHusb = optHusb.get().getLocationOrOriginal().getX();
            final var optWife = findIdvByIdx(fml.wife);
            final var xWife = optWife.get().getLocationOrOriginal().getX();
            if (xHusb <= xWife) {
                ret = optWife;
            } else {
                ret = optHusb;
            }
        } else if (isPlacedIdv(fml.husb)) {
            ret = findIdvByIdx(fml.husb);
        } else if (isPlacedIdv(fml.wife)) {
            ret = findIdvByIdx(fml.wife);
        } else {
            ret = Optional.empty();
        }

        return ret;
    }

    private Optional<Individual> findRightmostPlacedChildOf(final Individual idv) {
        return findRightmost(idv.ridxChild);
    }

    private Optional<Individual> findRightmostPlacedChildOf(final Family fml) {
        return findRightmost(fml.children);
    }

    private Optional<Individual> findLeftmost(final List<Integer> ridx) {
        return ridx
            .stream()
            .map(this::findIdvByIdx)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(Individual::isPlaced)
            .min(Comparator.comparing(idv -> idv.getLocationOrOriginal().getX()));
    }

    private Optional<Individual> findRightmost(final List<Integer> ridx) {
        return ridx
            .stream()
            .map(this::findIdvByIdx)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(Individual::isPlaced)
            .max(Comparator.comparing(idv -> idv.getLocationOrOriginal().getX()));
    }

    private Optional<Individual> findTopmost(final List<Integer> ridx) {
        return ridx
            .stream()
            .map(this::findIdvByIdx)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(Individual::isPlaced)
            .min(Comparator.comparing(idv -> idv.getLocationOrOriginal().getY()));
    }

    private Optional<Individual> findBottommost(final List<Integer> ridx) {
        return ridx
            .stream()
            .map(this::findIdvByIdx)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(Individual::isPlaced)
            .max(Comparator.comparing(idv -> idv.getLocationOrOriginal().getY()));
    }




    private Point2D getBottomLeftPositionForChild() {
        return GEN_BELOW.applyTo(getBottomLeftPosition());
    }

    private Point2D getBottomLeftPositionForParent() {
        return BELOW.applyTo(getBottomLeftPosition());
    }

    private Point2D getBottomLeftPosition() {
        final var idvLowest = getLowest().getLocationOrOriginal();
        final var idvLeftmost = getLeftmost().getLocationOrOriginal();
        return new Point2D(idvLeftmost.getX(), idvLowest.getY());
    }

    private Individual getLowest() {
        return this.indis
            .stream()
            .filter(Individual::isPlaced)
            .max(Comparator.comparing(idv -> idv.getLocationOrOriginal().getY()))
            .get();
    }

    private Individual getLeftmost() {
        return this.indis
            .stream()
            .filter(Individual::isPlaced)
            .min(Comparator.comparing(idv -> idv.getLocationOrOriginal().getX()))
            .get();
    }




    private static void placeIdvToRightOfOther(final Individual idv, final Individual other) {
        placeIdvRelativeToOther(idv, RIGHT_OF, other);
    }

    private static void placeIdvBelowOther(final Individual idv, final Individual other) {
        placeIdvRelativeToOther(idv, BELOW, other);
    }

    private static void placeIdvRelativeToOther(final Individual idv, final Translation t, final Individual other) {
        final Point2D q = t.applyTo(other.getLocationOrOriginal());
        idv.moveToAndLayOut(q);
    }

    private boolean isPlacedIdv(final int iIdvOrNegOne) {
        final var optIdv = findIdvByIdx(iIdvOrNegOne);
        return optIdv.isPresent() && optIdv.get().isPlaced();
    }

    /**
     * Looks up a Layout.Individual based on its index. Note: index variables
     * typically have a value of -1 to indicate being uninitialized, meaning
     * pointing to no individual. This function incorporates the use of
     * Optional to handle this case safely.
     *
     * @param idx index into list of all Individual objects
     * @return the Individual, if found, or empty() if not.
     */
    private Optional<Individual> findIdvByIdx(final int idx) {
        final Optional<Individual> ret;
        final int n = this.indis.size();
        if (0 <= idx && idx < n) {
            ret = Optional.of(this.indis.get(idx));
        } else {
            ret = Optional.empty();
        }
        return ret;
    }












// FULL LAYOUT

    public void cleanAll() {
        if (this.indis.size() <= 1) {
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
                        setIslandLevels(indi, batch++ * 5);
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
                    fami.getSortedChildren(riChild);
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
