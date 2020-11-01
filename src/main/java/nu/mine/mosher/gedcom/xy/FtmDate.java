package nu.mine.mosher.gedcom.xy;



import org.slf4j.*;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.temporal.JulianFields;
import java.util.Objects;
import java.util.regex.*;



public class FtmDate implements Comparable<FtmDate> {
    private static final Logger LOG =  LoggerFactory.getLogger(FtmDate.class);
    private static final FlaggedDate FD_UNKNOWN = new FlaggedDate(0x80000011);

    private final FlaggedDate earliest;
    private final FlaggedDate latest;
    private final String other;

    public static final FtmDate UNKNOWN = new FtmDate(FD_UNKNOWN, FD_UNKNOWN);

    private FtmDate(final long d1, final long d2, final String other) {
        FlaggedDate fd1 = new FlaggedDate(d1);
        FlaggedDate fd2 = new FlaggedDate(d2);

        // doctor up "after/before" flags into earliest/latest dates
        if (fd1.equals(fd2) && !fd1.about) {
            if (fd1.after) {
                fd2 = FD_UNKNOWN;
            } else if (fd1.before) {
                fd2 = fd1;
                fd1 = FD_UNKNOWN;
            }
        }

        this.earliest = fd1;
        this.latest = fd2;
        this.other = other;
    }

    private FtmDate(FlaggedDate fd1, FlaggedDate fd2) {
        this.earliest = fd1;
        this.latest = fd2;
        this.other = "";
    }

    @Override
    public String toString() {
        if (this.earliest.unknown() && this.latest.unknown()) {
            return "?";
        }

        if (this.earliest.unknown()) {
            return "?"+this.latest.asSimpleYear();
        }
        if (this.latest.unknown()) {
            return this.earliest.asSimpleYear()+"?";
        }

        if (!this.earliest.equals(this.latest)) {
            return ""+(this.earliest.asSimpleYear()+this.latest.asSimpleYear())/2+"?";
        }

        return ""+this.earliest.asSimpleYear()+(this.earliest.about() ? "?" : "");
    }

    public long ym() {
        if (this.earliest.unknown() && this.latest.unknown()) {
            return 0L;
        }

        if (this.earliest.unknown()) {
            return this.latest.asSimpleYM();
        }
        if (this.latest.unknown()) {
            return this.earliest.asSimpleYM();
        }

        if (!this.earliest.equals(this.latest)) {
            return (this.earliest.asSimpleYM()+this.latest.asSimpleYM())/2L;
        }

        return this.earliest.asSimpleYM();
    }

    private static final Pattern ONE_DATE = Pattern.compile("^(\\d+)$");
    private static final Pattern TWO_DATES = Pattern.compile("^(\\d+):(\\d+)$");

    public static FtmDate fromFtmFactDate(final String d) {
        if (Objects.isNull(d)) {
            return UNKNOWN;
        }
        Matcher m;
        if ((m = ONE_DATE.matcher(d)).matches()) {
            final long date = Long.parseLong(m.group(1), 10);
            return new FtmDate(date, date, "");
        } else if ((m = TWO_DATES.matcher(d)).matches()) {
            final long date1 = Long.parseLong(m.group(1), 10);
            final long date2 = Long.parseLong(m.group(2), 10);
            return new FtmDate(date1, date2, "");
        } else {
            return new FtmDate(0, 0, d);
        }
    }

    @Override
    // not consistent with equals
    public int compareTo(FtmDate that) {
        return this.earliest.compareTo(that.earliest);
    }

    public boolean isRecent() {
        return this.latest.isRecent() || this.earliest.isRecent();
    }

    public boolean unknown() {
        return this.earliest.unknown() && this.latest.unknown();
    }

    private static class FlaggedDate implements Comparable<FlaggedDate> {
        private final long flags;
        private final boolean unknown;
        private final long d;
        private final LocalDate ld;
        private final boolean before;
        private final boolean after;
        private final boolean about;
        private final boolean dualYear;
        private final boolean noYear;
        private final boolean noMonth;
        private final boolean noDay;
        private final boolean calculated;

        private FlaggedDate(final long n) {
            this.flags = n & 0x1FF;
            this.unknown = (Integer.MIN_VALUE & n) != 0;
            this.d = (Integer.MAX_VALUE & n) >> 9;

            this.ld = LocalDate.MIN.with(JulianFields.JULIAN_DAY, this.d);
            final BigInteger f = BigInteger.valueOf(this.flags);
            this.before = f.testBit(0);
            this.after = f.testBit(1);
            this.about = this.before && this.after;
            this.dualYear = f.testBit(4);
            this.noYear = f.testBit(5);
            this.noMonth = f.testBit(6);
            this.noDay = f.testBit(7);
            this.calculated = f.testBit(8);
        }

        public boolean unknown() {
            return this.unknown;
        }

        public boolean about() {
            return this.about;
        }

        public int asSimpleYear() {
            if (this.unknown || this.noYear) {
                return 0;
            }
            int year = this.ld.getYear();
            if (year < 0) {
                return year-1;
            }
            return year;
        }

        public long asSimpleYM() {
            final long y = asSimpleYear();
            if (y == 0) {
                return 0;
            }
            return y*100L+(this.noMonth ? 6L : this.ld.getMonthValue());
        }

        @Override
        // not consistent with equals
        public int compareTo(final FlaggedDate o) {
            return Long.compare(this.d, o.d);
        }

        @Override
        public boolean equals(Object o) {
            if (Objects.isNull(o) || !(o instanceof FlaggedDate)) {
                return false;
            }
            final FlaggedDate that = (FlaggedDate)o;
            return this.flags == that.flags && this.unknown == that.unknown && this.d == that.d;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.flags, this.unknown, this.d);
        }

        // TODO parameterize years for recency?
        // TODO implement privatization based on database columns in tables:
        // Person, Relationship, ChildRelationship, Fact, Note, MediaLink, MediaFile
        public boolean isRecent() {
            return  !this.unknown && LocalDate.now().minusYears(110).compareTo(this.ld) < 0;
        }
    }
}
