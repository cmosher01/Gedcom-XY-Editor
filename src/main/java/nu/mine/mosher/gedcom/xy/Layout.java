/*
 *     Copyright © 2018-2026, Christopher Alan Mosher, New York, New York, USA, <cmosher01@gmail.com>.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Layout {
    private static final Logger LOG = LoggerFactory.getLogger(Layout.class);
    private static final double MAX_LEVEL = 5000.0D;
    private static final double MAX_WIDTH = 54D; // TODO calculate max width?
    private static final double GEN_HEIGHT = 108D;
    private static final double DX_INDIVIDUAL = 1.2D * MAX_WIDTH;
    private static final double DY_INDIVIDUAL = GEN_HEIGHT; // TODO is this good?
    private static final double DX_FAMILY = 5.0D * MAX_WIDTH;
    private static final Translation RIGHT_OF = new Translation(DX_INDIVIDUAL, 0.0D);
    private static final Translation ABOVE = new Translation(0.0D, -DY_INDIVIDUAL);
    private static final Translation BELOW = new Translation(0.0D, DY_INDIVIDUAL);
    private static final Translation GEN_BELOW = new Translation(0.0D, 1.2D * DY_INDIVIDUAL);



    private final List<Individual> indis;
    private final List<Family> famis;



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
            f.husb = getIdxOfSpouseOrNegativeOne(mapIndis, husb);
            final Optional<Indi> wife = fami.getWife();
            f.wife = getIdxOfSpouseOrNegativeOne(mapIndis, wife);

            addSpouse(mapIndis, f, husb, wife);
            addSpouse(mapIndis, f, wife, husb);

            fami.getChildren().forEach(c -> {
                final Individual cChild = mapIndis.get(c);
                cChild.idxChildToFamily = f.idx;
                f.ridxChild.add(cChild.idx);
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









    record LevelSetting(Individual indi, int level) { }



    class Family {
        private final int idx;
        private final Fami fami;

        /* indexes into indis list */
        private int husb = -1;
        private int wife = -1;
        private final List<Integer> ridxChild = new ArrayList<>();
        private List<Integer> ridxChildSortedCache = null;


        public Family(final Fami fami, final int idx) {
            this.fami = fami;
            this.idx = idx;
        }

        public List<Integer> getSortedChildren() {
            if (Objects.isNull(this.ridxChildSortedCache)) {
                final var newList = new ArrayList<>(this.ridxChild);
                newList.sort(Comparator.comparing(i -> Layout.this.indis.get(i).getBirthForSort()));
                this.ridxChildSortedCache = Collections.unmodifiableList(newList);
            }
            return this.ridxChildSortedCache;
        }





// used to classify families into use cases concerning placed vs. unplaced parents and children

        public boolean hasPlacedChildren() {
            for (final var i : this.ridxChild) {
                final var optIdv = findIdvByIdx(i);
                if (optIdv.isPresent() && optIdv.get().isPlaced()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * count of parents
         * @return 0, 1, or 2
         */
        public int cParents() {
            final var optHusb = findIdvByIdx(this.husb);
            final var optWife = findIdvByIdx(this.wife);

            int c = 0;
            if (optHusb.isPresent()) {
                c++;
            }
            if (optWife.isPresent()) {
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
            final var optHusb = findIdvByIdx(this.husb);
            final var optWife = findIdvByIdx(this.wife);

            int c = 0;
            if (optHusb.isPresent() && optHusb.get().isPlaced()) {
                c++;
            }
            if (optWife.isPresent() && optWife.get().isPlaced()) {
                c++;
            }
            return c;
        }

        /**
         * Gets one of the parents, if any, preferring the husband.
         *
         * @return husband if present, otherwise wife if present, otherwise empty
         */
        private Optional<Individual> preferHusband() {
            final var optHusb = findIdvByIdx(this.husb);
            final var optWife = findIdvByIdx(this.wife);

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

        /**
         * Finds the "first" child, based on the birthdate
         * (if there are any children at all)
         * @return first child if any, otherwise empty
         */
        private Optional<Individual> firstChild() {
            // Get first child in fml.
            final var riChild = getSortedChildren();
            if (riChild.isEmpty()) {
                return Optional.empty();
            }

            return findIdvByIdx(riChild.getFirst());
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

        // "levels" are used during a full cleaning (not a partial cleaning) to indicate
        // what "generation" the Individual is in, and therefore dictate its placement
        // within the chart along the Y axis.

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
                for (int i = 0; i < fami.ridxChild.size(); ++i) {
                    levelSettingDeque.offer(new LevelSetting(layout.indis.get(fami.ridxChild.get(i)), lev));
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

















    public void clean() {
        if (this.indis.size() <= 1) {
            return;
        }

        if (this.indis.stream().noneMatch(i -> i.getOrigIndi().hadOriginalXY())) {
            cleanAll(); // FULL LAYOUT
        } else {
            cleanUnplaced(); // PARTIAL LAYOUT
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
        with other people in the tree? How would we determine where to move them to?
        And/or detect overlapping people and flag with colors or something.





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
        P.  At least one placed child, and either no other children
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

    public void cleanUnplaced() {
        this.famis.forEach(this::cleanFamily);
    }

    private void cleanFamily(final Family fml) {
        // WARNING: values within each "if" block can modify the
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





    // U1 . No parents in family.
    //      No children, or only unplaced children, in family.
    //
    //      Place first child (if any) below bottom of tree.
    //
    // assume there are no placed children (i.e., no children, or only unplaced children)
    private void placeFirstChildOfFamilyAtBottom(final Family fml) {
        final var optFirstChild = fml.firstChild();
        if (optFirstChild.isPresent()) {
            optFirstChild.get().moveToAndLayOut(GEN_BELOW.applyTo(getBottomLeftPosition()));
        }
    }

    // U2a. Exactly one parent in family, who is unplaced.
    //      No children, or only unplaced children, in family.
    //
    //      Place parent below bottom of tree.
    //
    // U3a. Two parents in family, neither placed.
    //      No children, or only unplaced children, in family.
    //
    //      Place husband below bottom of tree (or next to spouse, if any other???)
    private void placeParentAtBottom(final Family fml) {
        final Optional<Individual> optParent = fml.preferHusband();
        if (optParent.isPresent()) {
            optParent.get().moveToAndLayOut(BELOW.applyTo(getBottomLeftPosition()));
        }
    }

    // U2b. Exactly one parent in family, who is placed.
    //      No children, or only unplaced children, in family.
    //
    //      Place first child (if any) below parent.
    //
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

        // calculate new position: (left-most parent x, bottom-most parent y)
        final var bottomleft = new Point2D(
            optLeftmostParent.get().getLocationOrOriginal().getX(),
            optBottomParent.get().getLocationOrOriginal().getY());

        // Get first child in fml.
        final var optFirstChild = fml.firstChild();
        if (optFirstChild.isEmpty()) {
            // should never happen
            return;
        }

        // Move first child to below bottom-left of parents
        optFirstChild.get().moveToAndLayOut(BELOW.applyTo(bottomleft));
    }

    // U3b. Two parents in family, exactly one placed.
    //      No children, or only unplaced children, in family.
    //
    //      Place unplaced parent to right of placed parent.
    //
    // P3b. Two parents in family, exactly one placed.
    //      At least one placed child within family
    //
    //      Place unplaced parent to right of placed parent.
    private void placeUnplacedSpouseToRightOfPlacedSpouse(final Family fml) {
        final var optHusb = findIdvByIdx(fml.husb);
        if (optHusb.isEmpty()) {
            return; // should never happen
        }
        final var h = optHusb.get();
        final var optWife = findIdvByIdx(fml.wife);
        if (optWife.isEmpty()) {
            return; // should never happen
        }
        final var w = optWife.get();

        if (h.isPlaced()) {
            w.moveToAndLayOut(RIGHT_OF.applyTo(h.getLocationOrOriginal()));
        } else {
            h.moveToAndLayOut(RIGHT_OF.applyTo(w.getLocationOrOriginal()));
        }
    }

    // P1 . No parents in family.
    //      At least one placed child within family
    //
    //      Place any and all unplaced other children to right of rightmost placed child.
    //
    // P2b. Exactly one parent in family, who is placed.
    //      At least one placed child within family
    //
    //      Place any and all unplaced other children to right of rightmost placed child.
    //
    // P3c. Two parents in family, both placed.
    //      At least one placed child within family
    //
    //      Place any and all unplaced other children to right of rightmost placed child.
    private void placeSubsequentUnplacedChildrenOfFamily(final Family fml) {
        final var optRight = findRightmost(fml.ridxChild);
        if (optRight.isEmpty()) { // probably shouldn't ever happen
            return;
        }

        final var riChild = fml.getSortedChildren();

        // prevChild starts as the rightmost placed child, and
        // then walks through each unplaced child as it gets placed
        Individual prevChild = optRight.get();
        for (final int iChild : riChild) {
            final var opt = findIdvByIdx(iChild);
            if (opt.isPresent() && !opt.get().isPlaced()) {
                final var currChild = opt.get();
                currChild.moveToAndLayOut(RIGHT_OF.applyTo(prevChild.getLocationOrOriginal()));
                prevChild = currChild;
            }
        }
    }

    // P2a. Exactly one parent in family, who is unplaced.
    //      At least one placed child within family
    //
    //      Place parent (at leftmost placed child x, above topmost placed child y).
    //
    // P3a. Two parents in family, neither placed.
    //      At least one placed child within family
    //
    //      Place husband (at leftmost placed child x, above topmost placed child y).
    //
    // if there are no placed children, does nothing
    private void placeParentAboveChildren(final Family fml) {
        final var optLeftmost = findLeftmost(fml.ridxChild);
        final var optTopmost = findTopmost(fml.ridxChild);
        if (optLeftmost.isPresent() && optTopmost.isPresent()) {
            final var topleft = new Point2D(
                optLeftmost.get().getLocationOrOriginal().getX(),
                optTopmost.get().getLocationOrOriginal().getY());

            final Optional<Individual> optParent = fml.preferHusband();
            if (optParent.isPresent()) {
                optParent.get().moveToAndLayOut(ABOVE.applyTo(topleft));
            }
        }
    }



    // these four "find{Xxx}most" functions aren't particularly efficient,
    // but they are only ever called with a handful of indexes.

    private Optional<Individual> findLeftmost(final List<Integer> ridx) {
        return streamPlacedIndividuals(ridx).min(comparingX());
    }

    private Optional<Individual> findRightmost(final List<Integer> ridx) {
        return streamPlacedIndividuals(ridx).max(comparingX());
    }

    private Optional<Individual> findTopmost(final List<Integer> ridx) {
        return streamPlacedIndividuals(ridx).min(comparingY());
    }

    private Optional<Individual> findBottommost(final List<Integer> ridx) {
        return streamPlacedIndividuals(ridx).max(comparingY());
    }

    private static Comparator<Individual> comparingX() {
        return Comparator.comparing(idv -> idv.getLocationOrOriginal().getX());
    }

    private static Comparator<Individual> comparingY() {
        return Comparator.comparing(idv -> idv.getLocationOrOriginal().getY());
    }

    private Stream<Individual> streamPlacedIndividuals(final List<Integer> ridx) {
        return
            ridx
            .stream()
            .map(this::findIdvByIdx)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(Individual::isPlaced);
    }



    /**
     * Gets the maximum Y and the minimum X positions
     * of all the laid out Individuals.
     *
     * @return point (minx,maxy)
     */
    private Point2D getBottomLeftPosition() {
        double minx = Double.POSITIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        for (final var idv : this.indis) {
            final var pos = idv.getLocationOrOriginal();
            if (!pos.equals(Point2D.ZERO)) {
                final double x = pos.getX();
                if (Double.compare(x, minx) < 0) {
                    minx = x;
                }
                final double y = pos.getY();
                if (Double.compare(maxy, y) < 0) {
                    maxy = y;
                }
            }
        }
        return new Point2D(minx, maxy);
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
        var ret = Optional.<Individual>empty();
        if (0 <= idx && idx < this.indis.size()) {
            ret = Optional.of(this.indis.get(idx));
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
            resetMarks();
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
            resetMarks();
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

        resetMarks();

        for (final Individual indi : qToClean) {
            final Deque<Individual> todo = new LinkedList<>();

            indi.setRootWithSpouses(indi);
            todo.addLast(indi);
            while (!todo.isEmpty()) {
                final Individual pgmi = todo.removeFirst();
                for (int j = 0; j < pgmi.ridxSpouseToFamily.size(); ++j) {
                    final Family fami = this.famis.get(pgmi.ridxSpouseToFamily.get(j));
                    for (int k = 0; k < fami.ridxChild.size(); ++k) {
                        final Individual pchil = this.indis.get(fami.ridxChild.get(k));
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

        resetMarks();
        LOG.debug("Moving branches");
        while (!rptoclean2.isEmpty()) {
            final Individual psec = rptoclean2.remove();
            settoclean2.remove(psec);
            LOG.debug("branch head: {}", psec.indi.nameSimple());

            final List<Individual> nexthouse = new ArrayList<>();

            final Set<Individual> guard = new HashSet<>();
            final List<Individual> todo = new ArrayList<>();
            todo.add(psec);
            guard.add(psec);
            while (!todo.isEmpty()) {
                final Individual pgmi = todo.removeFirst();
                final List<Individual> cleannext = new ArrayList<>();
                pgmi.setSeqWithSpouses(xForLevel, false, cleannext);
                nexthouse.addAll(cleannext);

                for (int j = 0; j < pgmi.ridxSpouseToFamily.size(); ++j) {
                    final Family fami = this.famis.get(pgmi.ridxSpouseToFamily.get(j));
                    final List<Integer> riChild = fami.getSortedChildren();
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

        normalizeAllIndiCoordinates();

        this.indis.forEach(Individual::layOut);
    }

    private void normalizeAllIndiCoordinates() {
        /*
        After a full layout, individuals at the bottom of the chart
        have Y coordinates in the 500,000's, and almost always a huge
        empty area above all individuals. This is due to the full
        layout algorithm that stars at the theoretical maximum
        "bottom" of chart with MAX_LEVEL (5000) generations.
         */
        final double x = this.indis.stream().map(Individual::getLocationOrOriginal).mapToDouble(Point2D::getX).min().orElse(0D);
        final double y = this.indis.stream().map(Individual::getLocationOrOriginal).mapToDouble(Point2D::getY).min().orElse(0D);
        final Point2D coordsTopLeft = new Point2D(x, y);
        this.indis.forEach(i -> i.location = i.getLocationOrOriginal().subtract(coordsTopLeft));
    }

    private void resetMarks() {
        this.indis.forEach(i -> i.mark = false);
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



    private static int getIdxOfSpouseOrNegativeOne(final Map<Indi, Individual> mapIndis, final Optional<Indi> spouse) {
        if (spouse.isEmpty()) {
            return -1;
        }
        final var optIdv = Optional.ofNullable(mapIndis.get(spouse.get()));
        return optIdv.map(i -> i.idx).orElse(-1);
    }

    private static void addSpouse(Map<Indi, Individual> mapIndis, Family fami, Optional<Indi> indi, Optional<Indi> spouse) {
        if (indi.isPresent()) {
            final Individual cindi = mapIndis.get(indi.get());
            cindi.ridxSpouseToFamily.add(fami.idx);
            spouse.ifPresent(i -> cindi.ridxSpouse.add(mapIndis.get(i).idx));
        }
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
}
