/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.test;

import org.apache.calcite.avatica.util.ByteString;
import org.apache.calcite.avatica.util.DateTimeUtils;
import org.apache.calcite.runtime.CalciteException;
import org.apache.calcite.runtime.SqlFunctions;
import org.apache.calcite.runtime.Utilities;

import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.calcite.avatica.util.DateTimeUtils.ymdToUnixDate;
import static org.apache.calcite.runtime.SqlFunctions.addMonths;
import static org.apache.calcite.runtime.SqlFunctions.bitwiseAnd;
import static org.apache.calcite.runtime.SqlFunctions.bitwiseOR;
import static org.apache.calcite.runtime.SqlFunctions.bitwiseSHL;
import static org.apache.calcite.runtime.SqlFunctions.bitwiseSHR;
import static org.apache.calcite.runtime.SqlFunctions.bitwiseXOR;
import static org.apache.calcite.runtime.SqlFunctions.charLength;
import static org.apache.calcite.runtime.SqlFunctions.charindex;
import static org.apache.calcite.runtime.SqlFunctions.concat;
import static org.apache.calcite.runtime.SqlFunctions.cotFunction;
import static org.apache.calcite.runtime.SqlFunctions.dateMod;
import static org.apache.calcite.runtime.SqlFunctions.datetimeAdd;
import static org.apache.calcite.runtime.SqlFunctions.datetimeSub;
import static org.apache.calcite.runtime.SqlFunctions.dayNumberOfCalendar;
import static org.apache.calcite.runtime.SqlFunctions.dayOccurrenceOfMonth;
import static org.apache.calcite.runtime.SqlFunctions.format;
import static org.apache.calcite.runtime.SqlFunctions.fromBase64;
import static org.apache.calcite.runtime.SqlFunctions.greater;
import static org.apache.calcite.runtime.SqlFunctions.ifNull;
import static org.apache.calcite.runtime.SqlFunctions.initcap;
import static org.apache.calcite.runtime.SqlFunctions.instr;
import static org.apache.calcite.runtime.SqlFunctions.isNull;
import static org.apache.calcite.runtime.SqlFunctions.lesser;
import static org.apache.calcite.runtime.SqlFunctions.lower;
import static org.apache.calcite.runtime.SqlFunctions.lpad;
import static org.apache.calcite.runtime.SqlFunctions.ltrim;
import static org.apache.calcite.runtime.SqlFunctions.md5;
import static org.apache.calcite.runtime.SqlFunctions.monthNumberOfQuarter;
import static org.apache.calcite.runtime.SqlFunctions.monthNumberOfYear;
import static org.apache.calcite.runtime.SqlFunctions.nvl;
import static org.apache.calcite.runtime.SqlFunctions.octetLength;
import static org.apache.calcite.runtime.SqlFunctions.posixRegex;
import static org.apache.calcite.runtime.SqlFunctions.quarterNumberOfYear;
import static org.apache.calcite.runtime.SqlFunctions.regexpContains;
import static org.apache.calcite.runtime.SqlFunctions.regexpExtract;
import static org.apache.calcite.runtime.SqlFunctions.regexpMatchCount;
import static org.apache.calcite.runtime.SqlFunctions.regexpReplace;
import static org.apache.calcite.runtime.SqlFunctions.rpad;
import static org.apache.calcite.runtime.SqlFunctions.rtrim;
import static org.apache.calcite.runtime.SqlFunctions.sha1;
import static org.apache.calcite.runtime.SqlFunctions.strTok;
import static org.apache.calcite.runtime.SqlFunctions.substring;
import static org.apache.calcite.runtime.SqlFunctions.subtractMonths;
import static org.apache.calcite.runtime.SqlFunctions.timeSub;
import static org.apache.calcite.runtime.SqlFunctions.timestampToDate;
import static org.apache.calcite.runtime.SqlFunctions.toBase64;
import static org.apache.calcite.runtime.SqlFunctions.toBinary;
import static org.apache.calcite.runtime.SqlFunctions.toCharFunction;
import static org.apache.calcite.runtime.SqlFunctions.toVarchar;
import static org.apache.calcite.runtime.SqlFunctions.trim;
import static org.apache.calcite.runtime.SqlFunctions.upper;
import static org.apache.calcite.runtime.SqlFunctions.weekNumberOfCalendar;
import static org.apache.calcite.runtime.SqlFunctions.weekNumberOfMonth;
import static org.apache.calcite.runtime.SqlFunctions.weekNumberOfYear;
import static org.apache.calcite.runtime.SqlFunctions.yearNumberOfCalendar;
import static org.apache.calcite.test.Matchers.within;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Unit test for the methods in {@link SqlFunctions} that implement SQL
 * functions.
 *
 * <p>Developers, please use {@link org.junit.Assert#assertThat assertThat}
 * rather than {@code assertEquals}.
 */
public class SqlFunctionsTest {
  @Test public void testCharLength() {
    assertThat(charLength("xyz"), is(3));
  }

  @Test public void testConcat() {
    assertThat(concat("a b", "cd"), is("a bcd"));
    // The code generator will ensure that nulls are never passed in. If we
    // pass in null, it is treated like the string "null", as the following
    // tests show. Not the desired behavior for SQL.
    assertThat(concat("a", null), is("anull"));
    assertThat(concat((String) null, null), is("nullnull"));
    assertThat(concat(null, "b"), is("nullb"));
  }

  @Test public void testPosixRegex() {
    assertThat(posixRegex("abc", "abc", true), is(true));
    assertThat(posixRegex("abc", "^a", true), is(true));
    assertThat(posixRegex("abc", "(b|d)", true), is(true));
    assertThat(posixRegex("abc", "^(b|c)", true), is(false));

    assertThat(posixRegex("abc", "ABC", false), is(true));
    assertThat(posixRegex("abc", "^A", false), is(true));
    assertThat(posixRegex("abc", "(B|D)", false), is(true));
    assertThat(posixRegex("abc", "^(B|C)", false), is(false));

    assertThat(posixRegex("abc", "^[[:xdigit:]]$", false), is(false));
    assertThat(posixRegex("abc", "^[[:xdigit:]]+$", false), is(true));
    assertThat(posixRegex("abcq", "^[[:xdigit:]]+$", false), is(false));

    assertThat(posixRegex("abc", "[[:xdigit:]]", false), is(true));
    assertThat(posixRegex("abc", "[[:xdigit:]]+", false), is(true));
    assertThat(posixRegex("abcq", "[[:xdigit:]]", false), is(true));
  }

  @Test public void testRegexpReplace() {
    assertThat(regexpReplace("a b c", "b", "X"), is("a X c"));
    assertThat(regexpReplace("abc def ghi", "[g-z]+", "X"), is("abc def X"));
    assertThat(regexpReplace("abc def ghi", "[a-z]+", "X"), is("X X X"));
    assertThat(regexpReplace("a b c", "a|b", "X"), is("X X c"));
    assertThat(regexpReplace("a b c", "y", "X"), is("a b c"));

    assertThat(regexpReplace("100-200", "(\\d+)", "num"), is("num-num"));
    assertThat(regexpReplace("100-200", "(\\d+)", "###"), is("###-###"));
    assertThat(regexpReplace("100-200", "(-)", "###"), is("100###200"));

    assertThat(regexpReplace("abc def ghi", "[a-z]+", "X", 1), is("X X X"));
    assertThat(regexpReplace("abc def ghi", "[a-z]+", "X", 2), is("aX X X"));
    assertThat(regexpReplace("abc def ghi", "[a-z]+", "X", 1, 3),
        is("abc def X"));
    assertThat(regexpReplace("abc def GHI", "[a-z]+", "X", 1, 3, "c"),
        is("abc def GHI"));
    assertThat(regexpReplace("abc def GHI", "[a-z]+", "X", 1, 3, "i"),
        is("abc def X"));

    try {
      regexpReplace("abc def ghi", "[a-z]+", "X", 0);
      fail("'regexp_replace' on an invalid pos is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Not a valid input for REGEXP_REPLACE: '0'"));
    }

    try {
      regexpReplace("abc def ghi", "[a-z]+", "X", 1, 3, "WWW");
      fail("'regexp_replace' on an invalid matchType is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Not a valid input for REGEXP_REPLACE: 'WWW'"));
    }
  }

  @Test public void testLower() {
    assertThat(lower("A bCd Iijk"), is("a bcd iijk"));
  }

  @Test public void testFromBase64() {
    final List<String> expectedList =
        Arrays.asList("", "\0", "0", "a", " ", "\n", "\r\n", "\u03C0",
            "hello\tword");
    for (String expected : expectedList) {
      assertThat(fromBase64(toBase64(expected)),
          is(new ByteString(expected.getBytes(UTF_8))));
    }
    assertThat("546869732069732061207465737420537472696e672e",
        is(fromBase64("VGhpcyB  pcyBh\rIHRlc3Qg\tU3Ry\naW5nLg==").toString()));
    assertThat(fromBase64("-1"), nullValue());
  }

  @Test public void testToBase64() {
    final String s = ""
        + "This is a test String. check resulte out of 76This is a test String."
        + "This is a test String.This is a test String.This is a test String."
        + "This is a test String. This is a test String. check resulte out of 76"
        + "This is a test String.This is a test String.This is a test String."
        + "This is a test String.This is a test String. This is a test String. "
        + "check resulte out of 76This is a test String.This is a test String."
        + "This is a test String.This is a test String.This is a test String.";
    final String actual = ""
        + "VGhpcyBpcyBhIHRlc3QgU3RyaW5nLiBjaGVjayByZXN1bHRlIG91dCBvZiA3NlRoaXMgaXMgYSB0\n"
        + "ZXN0IFN0cmluZy5UaGlzIGlzIGEgdGVzdCBTdHJpbmcuVGhpcyBpcyBhIHRlc3QgU3RyaW5nLlRo\n"
        + "aXMgaXMgYSB0ZXN0IFN0cmluZy5UaGlzIGlzIGEgdGVzdCBTdHJpbmcuIFRoaXMgaXMgYSB0ZXN0\n"
        + "IFN0cmluZy4gY2hlY2sgcmVzdWx0ZSBvdXQgb2YgNzZUaGlzIGlzIGEgdGVzdCBTdHJpbmcuVGhp\n"
        + "cyBpcyBhIHRlc3QgU3RyaW5nLlRoaXMgaXMgYSB0ZXN0IFN0cmluZy5UaGlzIGlzIGEgdGVzdCBT\n"
        + "dHJpbmcuVGhpcyBpcyBhIHRlc3QgU3RyaW5nLiBUaGlzIGlzIGEgdGVzdCBTdHJpbmcuIGNoZWNr\n"
        + "IHJlc3VsdGUgb3V0IG9mIDc2VGhpcyBpcyBhIHRlc3QgU3RyaW5nLlRoaXMgaXMgYSB0ZXN0IFN0\n"
        + "cmluZy5UaGlzIGlzIGEgdGVzdCBTdHJpbmcuVGhpcyBpcyBhIHRlc3QgU3RyaW5nLlRoaXMgaXMg\n"
        + "YSB0ZXN0IFN0cmluZy4=";
    assertThat(toBase64(s), is(actual));
    assertThat(toBase64(""), is(""));
  }

  @Test public void testUpper() {
    assertThat(upper("A bCd iIjk"), is("A BCD IIJK"));
  }

  @Test public void testInitcap() {
    assertThat(initcap("aA"), is("Aa"));
    assertThat(initcap("zz"), is("Zz"));
    assertThat(initcap("AZ"), is("Az"));
    assertThat(initcap("tRy a littlE  "), is("Try A Little  "));
    assertThat(initcap("won't it?no"), is("Won'T It?No"));
    assertThat(initcap("1A"), is("1a"));
    assertThat(initcap(" b0123B"), is(" B0123b"));
  }

  @Test public void testLesser() {
    assertThat(lesser("a", "bc"), is("a"));
    assertThat(lesser("bc", "ac"), is("ac"));
    try {
      Object o = lesser("a", null);
      fail("Expected NPE, got " + o);
    } catch (NullPointerException e) {
      // ok
    }
    assertThat(lesser(null, "a"), is("a"));
    assertThat(lesser((String) null, null), nullValue());
  }

  @Test public void testGreater() {
    assertThat(greater("a", "bc"), is("bc"));
    assertThat(greater("bc", "ac"), is("bc"));
    try {
      Object o = greater("a", null);
      fail("Expected NPE, got " + o);
    } catch (NullPointerException e) {
      // ok
    }
    assertThat(greater(null, "a"), is("a"));
    assertThat(greater((String) null, null), nullValue());
  }

  /** Test for {@link SqlFunctions#rtrim}. */
  @Test public void testRtrim() {
    assertThat(rtrim(""), is(""));
    assertThat(rtrim("    "), is(""));
    assertThat(rtrim("   x  "), is("   x"));
    assertThat(rtrim("   x "), is("   x"));
    assertThat(rtrim("   x y "), is("   x y"));
    assertThat(rtrim("   x"), is("   x"));
    assertThat(rtrim("x"), is("x"));
  }

  /** Test for {@link SqlFunctions#ltrim}. */
  @Test public void testLtrim() {
    assertThat(ltrim(""), is(""));
    assertThat(ltrim("    "), is(""));
    assertThat(ltrim("   x  "), is("x  "));
    assertThat(ltrim("   x "), is("x "));
    assertThat(ltrim("x y "), is("x y "));
    assertThat(ltrim("   x"), is("x"));
    assertThat(ltrim("x"), is("x"));
  }

  /** Test for {@link SqlFunctions#trim}. */
  @Test public void testTrim() {
    assertThat(trimSpacesBoth(""), is(""));
    assertThat(trimSpacesBoth("    "), is(""));
    assertThat(trimSpacesBoth("   x  "), is("x"));
    assertThat(trimSpacesBoth("   x "), is("x"));
    assertThat(trimSpacesBoth("   x y "), is("x y"));
    assertThat(trimSpacesBoth("   x"), is("x"));
    assertThat(trimSpacesBoth("x"), is("x"));
  }

  static String trimSpacesBoth(String s) {
    return trim(true, true, " ", s);
  }

  @Test public void testAddMonths() {
    checkAddMonths(2016, 1, 1, 2016, 2, 1, 1);
    checkAddMonths(2016, 1, 1, 2017, 1, 1, 12);
    checkAddMonths(2016, 1, 1, 2017, 2, 1, 13);
    checkAddMonths(2016, 1, 1, 2015, 1, 1, -12);
    checkAddMonths(2016, 1, 1, 2018, 10, 1, 33);
    checkAddMonths(2016, 1, 31, 2016, 4, 30, 3);
    checkAddMonths(2016, 4, 30, 2016, 7, 30, 3);
    checkAddMonths(2016, 1, 31, 2016, 2, 29, 1);
    checkAddMonths(2016, 3, 31, 2016, 2, 29, -1);
    checkAddMonths(2016, 3, 31, 2116, 3, 31, 1200);
    checkAddMonths(2016, 2, 28, 2116, 2, 28, 1200);
  }

  private void checkAddMonths(int y0, int m0, int d0, int y1, int m1, int d1,
      int months) {
    final int date0 = ymdToUnixDate(y0, m0, d0);
    final long date = addMonths(date0, months);
    final int date1 = ymdToUnixDate(y1, m1, d1);
    assertThat((int) date, is(date1));

    assertThat(subtractMonths(date1, date0),
        anyOf(is(months), is(months + 1)));
    assertThat(subtractMonths(date1 + 1, date0),
        anyOf(is(months), is(months + 1)));
    assertThat(subtractMonths(date1, date0 + 1),
        anyOf(is(months), is(months - 1)));
    assertThat(subtractMonths(d2ts(date1, 1), d2ts(date0, 0)),
        anyOf(is(months), is(months + 1)));
    assertThat(subtractMonths(d2ts(date1, 0), d2ts(date0, 1)),
        anyOf(is(months - 1), is(months), is(months + 1)));
  }

  /** Converts a date (days since epoch) and milliseconds (since midnight)
   * into a timestamp (milliseconds since epoch). */
  private long d2ts(int date, int millis) {
    return date * DateTimeUtils.MILLIS_PER_DAY + millis;
  }

  @Test public void testFloor() {
    checkFloor(0, 10, 0);
    checkFloor(27, 10, 20);
    checkFloor(30, 10, 30);
    checkFloor(-30, 10, -30);
    checkFloor(-27, 10, -30);
  }

  private void checkFloor(int x, int y, int result) {
    assertThat(SqlFunctions.floor(x, y), is(result));
    assertThat(SqlFunctions.floor((long) x, (long) y), is((long) result));
    assertThat(SqlFunctions.floor((short) x, (short) y), is((short) result));
    assertThat(SqlFunctions.floor((byte) x, (byte) y), is((byte) result));
    assertThat(
        SqlFunctions.floor(BigDecimal.valueOf(x), BigDecimal.valueOf(y)),
        is(BigDecimal.valueOf(result)));
  }

  @Test public void testCeil() {
    checkCeil(0, 10, 0);
    checkCeil(27, 10, 30);
    checkCeil(30, 10, 30);
    checkCeil(-30, 10, -30);
    checkCeil(-27, 10, -20);
    checkCeil(-27, 1, -27);
  }

  private void checkCeil(int x, int y, int result) {
    assertThat(SqlFunctions.ceil(x, y), is(result));
    assertThat(SqlFunctions.ceil((long) x, (long) y), is((long) result));
    assertThat(SqlFunctions.ceil((short) x, (short) y), is((short) result));
    assertThat(SqlFunctions.ceil((byte) x, (byte) y), is((byte) result));
    assertThat(
        SqlFunctions.ceil(BigDecimal.valueOf(x), BigDecimal.valueOf(y)),
        is(BigDecimal.valueOf(result)));
  }

  /** Unit test for
   * {@link Utilities#compare(java.util.List, java.util.List)}. */
  @Test public void testCompare() {
    final List<String> ac = Arrays.asList("a", "c");
    final List<String> abc = Arrays.asList("a", "b", "c");
    final List<String> a = Collections.singletonList("a");
    final List<String> empty = Collections.emptyList();
    assertThat(Utilities.compare(ac, ac), is(0));
    assertThat(Utilities.compare(ac, new ArrayList<>(ac)), is(0));
    assertThat(Utilities.compare(a, ac), is(-1));
    assertThat(Utilities.compare(empty, ac), is(-1));
    assertThat(Utilities.compare(ac, a), is(1));
    assertThat(Utilities.compare(ac, abc), is(1));
    assertThat(Utilities.compare(ac, empty), is(1));
    assertThat(Utilities.compare(empty, empty), is(0));
  }

  @Test public void testTruncateLong() {
    assertThat(SqlFunctions.truncate(12345L, 1000L), is(12000L));
    assertThat(SqlFunctions.truncate(12000L, 1000L), is(12000L));
    assertThat(SqlFunctions.truncate(12001L, 1000L), is(12000L));
    assertThat(SqlFunctions.truncate(11999L, 1000L), is(11000L));

    assertThat(SqlFunctions.truncate(-12345L, 1000L), is(-13000L));
    assertThat(SqlFunctions.truncate(-12000L, 1000L), is(-12000L));
    assertThat(SqlFunctions.truncate(-12001L, 1000L), is(-13000L));
    assertThat(SqlFunctions.truncate(-11999L, 1000L), is(-12000L));
  }

  @Test public void testTruncateInt() {
    assertThat(SqlFunctions.truncate(12345, 1000), is(12000));
    assertThat(SqlFunctions.truncate(12000, 1000), is(12000));
    assertThat(SqlFunctions.truncate(12001, 1000), is(12000));
    assertThat(SqlFunctions.truncate(11999, 1000), is(11000));

    assertThat(SqlFunctions.truncate(-12345, 1000), is(-13000));
    assertThat(SqlFunctions.truncate(-12000, 1000), is(-12000));
    assertThat(SqlFunctions.truncate(-12001, 1000), is(-13000));
    assertThat(SqlFunctions.truncate(-11999, 1000), is(-12000));

    assertThat(SqlFunctions.round(12345, 1000), is(12000));
    assertThat(SqlFunctions.round(12845, 1000), is(13000));
    assertThat(SqlFunctions.round(-12345, 1000), is(-12000));
    assertThat(SqlFunctions.round(-12845, 1000), is(-13000));
  }

  @Test public void testSTruncateDouble() {
    assertThat(SqlFunctions.struncate(12.345d, 3), within(12.345d, 0.001));
    assertThat(SqlFunctions.struncate(12.345d, 2), within(12.340d, 0.001));
    assertThat(SqlFunctions.struncate(12.345d, 1), within(12.300d, 0.001));
    assertThat(SqlFunctions.struncate(12.999d, 0), within(12.000d, 0.001));

    assertThat(SqlFunctions.struncate(-12.345d, 3), within(-12.345d, 0.001));
    assertThat(SqlFunctions.struncate(-12.345d, 2), within(-12.340d, 0.001));
    assertThat(SqlFunctions.struncate(-12.345d, 1), within(-12.300d, 0.001));
    assertThat(SqlFunctions.struncate(-12.999d, 0), within(-12.000d, 0.001));

    assertThat(SqlFunctions.struncate(12345d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12001d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000d, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.struncate(12000d, -5), within(0d, 0.001));
    assertThat(SqlFunctions.struncate(11999d, -3), within(11000d, 0.001));

    assertThat(SqlFunctions.struncate(-12345d, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000d, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-11999d, -3), within(-11000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000d, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000d, -5), within(0d, 0.001));
  }

  @Test public void testSTruncateLong() {
    assertThat(SqlFunctions.struncate(12345L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12001L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000L, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.struncate(12000L, -5), within(0d, 0.001));
    assertThat(SqlFunctions.struncate(11999L, -3), within(11000d, 0.001));

    assertThat(SqlFunctions.struncate(-12345L, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000L, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-11999L, -3), within(-11000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000L, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000L, -5), within(0d, 0.001));
  }

  @Test public void testSTruncateInt() {
    assertThat(SqlFunctions.struncate(12345, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12001, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.struncate(12000, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.struncate(12000, -5), within(0d, 0.001));
    assertThat(SqlFunctions.struncate(11999, -3), within(11000d, 0.001));

    assertThat(SqlFunctions.struncate(-12345, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.struncate(-11999, -3), within(-11000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.struncate(-12000, -5), within(0d, 0.001));
  }

  @Test public void testSRoundDouble() {
    assertThat(SqlFunctions.sround(12.345d, 3), within(12.345d, 0.001));
    assertThat(SqlFunctions.sround(12.345d, 2), within(12.350d, 0.001));
    assertThat(SqlFunctions.sround(12.345d, 1), within(12.300d, 0.001));
    assertThat(SqlFunctions.sround(12.999d, 2), within(13.000d, 0.001));
    assertThat(SqlFunctions.sround(12.999d, 1), within(13.000d, 0.001));
    assertThat(SqlFunctions.sround(12.999d, 0), within(13.000d, 0.001));

    assertThat(SqlFunctions.sround(-12.345d, 3), within(-12.345d, 0.001));
    assertThat(SqlFunctions.sround(-12.345d, 2), within(-12.350d, 0.001));
    assertThat(SqlFunctions.sround(-12.345d, 1), within(-12.300d, 0.001));
    assertThat(SqlFunctions.sround(-12.999d, 2), within(-13.000d, 0.001));
    assertThat(SqlFunctions.sround(-12.999d, 1), within(-13.000d, 0.001));
    assertThat(SqlFunctions.sround(-12.999d, 0), within(-13.000d, 0.001));

    assertThat(SqlFunctions.sround(12345d, -1), within(12350d, 0.001));
    assertThat(SqlFunctions.sround(12345d, -2), within(12300d, 0.001));
    assertThat(SqlFunctions.sround(12345d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12001d, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000d, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.sround(12000d, -5), within(0d, 0.001));
    assertThat(SqlFunctions.sround(11999d, -3), within(12000d, 0.001));

    assertThat(SqlFunctions.sround(-12345d, -1), within(-12350d, 0.001));
    assertThat(SqlFunctions.sround(-12345d, -2), within(-12300d, 0.001));
    assertThat(SqlFunctions.sround(-12345d, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000d, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-11999d, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000d, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.sround(-12000d, -5), within(0d, 0.001));
  }

  @Test public void testSRoundLong() {
    assertThat(SqlFunctions.sround(12345L, -1), within(12350d, 0.001));
    assertThat(SqlFunctions.sround(12345L, -2), within(12300d, 0.001));
    assertThat(SqlFunctions.sround(12345L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12001L, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000L, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.sround(12000L, -5), within(0d, 0.001));
    assertThat(SqlFunctions.sround(11999L, -3), within(12000d, 0.001));

    assertThat(SqlFunctions.sround(-12345L, -1), within(-12350d, 0.001));
    assertThat(SqlFunctions.sround(-12345L, -2), within(-12300d, 0.001));
    assertThat(SqlFunctions.sround(-12345L, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000L, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-11999L, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000L, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.sround(-12000L, -5), within(0d, 0.001));
  }

  @Test public void testSRoundInt() {
    assertThat(SqlFunctions.sround(12345, -1), within(12350d, 0.001));
    assertThat(SqlFunctions.sround(12345, -2), within(12300d, 0.001));
    assertThat(SqlFunctions.sround(12345, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12001, -3), within(12000d, 0.001));
    assertThat(SqlFunctions.sround(12000, -4), within(10000d, 0.001));
    assertThat(SqlFunctions.sround(12000, -5), within(0d, 0.001));
    assertThat(SqlFunctions.sround(11999, -3), within(12000d, 0.001));

    assertThat(SqlFunctions.sround(-12345, -1), within(-12350d, 0.001));
    assertThat(SqlFunctions.sround(-12345, -2), within(-12300d, 0.001));
    assertThat(SqlFunctions.sround(-12345, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-11999, -3), within(-12000d, 0.001));
    assertThat(SqlFunctions.sround(-12000, -4), within(-10000d, 0.001));
    assertThat(SqlFunctions.sround(-12000, -5), within(0d, 0.001));
  }

  @Test public void testByteString() {
    final byte[] bytes = {(byte) 0xAB, (byte) 0xFF};
    final ByteString byteString = new ByteString(bytes);
    assertThat(byteString.length(), is(2));
    assertThat(byteString.toString(), is("abff"));
    assertThat(byteString.toString(16), is("abff"));
    assertThat(byteString.toString(2), is("1010101111111111"));

    final ByteString emptyByteString = new ByteString(new byte[0]);
    assertThat(emptyByteString.length(), is(0));
    assertThat(emptyByteString.toString(), is(""));
    assertThat(emptyByteString.toString(16), is(""));
    assertThat(emptyByteString.toString(2), is(""));

    assertThat(ByteString.EMPTY, is(emptyByteString));

    assertThat(byteString.substring(1, 2).toString(), is("ff"));
    assertThat(byteString.substring(0, 2).toString(), is("abff"));
    assertThat(byteString.substring(2, 2).toString(), is(""));

    // Add empty string, get original string back
    assertSame(byteString.concat(emptyByteString), byteString);
    final ByteString byteString1 = new ByteString(new byte[]{(byte) 12});
    assertThat(byteString.concat(byteString1).toString(), is("abff0c"));

    final byte[] bytes3 = {(byte) 0xFF};
    final ByteString byteString3 = new ByteString(bytes3);

    assertThat(byteString.indexOf(emptyByteString), is(0));
    assertThat(byteString.indexOf(byteString1), is(-1));
    assertThat(byteString.indexOf(byteString3), is(1));
    assertThat(byteString3.indexOf(byteString), is(-1));

    thereAndBack(bytes);
    thereAndBack(emptyByteString.getBytes());
    thereAndBack(new byte[]{10, 0, 29, -80});

    assertThat(ByteString.of("ab12", 16).toString(16), equalTo("ab12"));
    assertThat(ByteString.of("AB0001DdeAD3", 16).toString(16),
        equalTo("ab0001ddead3"));
    assertThat(ByteString.of("", 16), equalTo(emptyByteString));
    try {
      ByteString x = ByteString.of("ABg0", 16);
      fail("expected error, got " + x);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), equalTo("invalid hex character: g"));
    }
    try {
      ByteString x = ByteString.of("ABC", 16);
      fail("expected error, got " + x);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), equalTo("hex string has odd length"));
    }

    final byte[] bytes4 = {10, 0, 1, -80};
    final ByteString byteString4 = new ByteString(bytes4);
    final byte[] bytes5 = {10, 0, 1, 127};
    final ByteString byteString5 = new ByteString(bytes5);
    final ByteString byteString6 = new ByteString(bytes4);

    assertThat(byteString4.compareTo(byteString5) > 0, is(true));
    assertThat(byteString4.compareTo(byteString6) == 0, is(true));
    assertThat(byteString5.compareTo(byteString4) < 0, is(true));
  }

  private void thereAndBack(byte[] bytes) {
    final ByteString byteString = new ByteString(bytes);
    final byte[] bytes2 = byteString.getBytes();
    assertThat(bytes, equalTo(bytes2));

    final String base64String = byteString.toBase64String();
    final ByteString byteString1 = ByteString.ofBase64(base64String);
    assertThat(byteString, equalTo(byteString1));
  }

  @Test public void testEqWithAny() {
    // Non-numeric same type equality check
    assertThat(SqlFunctions.eqAny("hello", "hello"), is(true));

    // Numeric types equality check
    assertThat(SqlFunctions.eqAny(1, 1L), is(true));
    assertThat(SqlFunctions.eqAny(1, 1.0D), is(true));
    assertThat(SqlFunctions.eqAny(1L, 1.0D), is(true));
    assertThat(SqlFunctions.eqAny(new BigDecimal(1L), 1), is(true));
    assertThat(SqlFunctions.eqAny(new BigDecimal(1L), 1L), is(true));
    assertThat(SqlFunctions.eqAny(new BigDecimal(1L), 1.0D), is(true));
    assertThat(SqlFunctions.eqAny(new BigDecimal(1L), new BigDecimal(1.0D)),
        is(true));

    // Non-numeric different type equality check
    assertThat(SqlFunctions.eqAny("2", 2), is(false));
  }

  @Test public void testNeWithAny() {
    // Non-numeric same type inequality check
    assertThat(SqlFunctions.neAny("hello", "world"), is(true));

    // Numeric types inequality check
    assertThat(SqlFunctions.neAny(1, 2L), is(true));
    assertThat(SqlFunctions.neAny(1, 2.0D), is(true));
    assertThat(SqlFunctions.neAny(1L, 2.0D), is(true));
    assertThat(SqlFunctions.neAny(new BigDecimal(2L), 1), is(true));
    assertThat(SqlFunctions.neAny(new BigDecimal(2L), 1L), is(true));
    assertThat(SqlFunctions.neAny(new BigDecimal(2L), 1.0D), is(true));
    assertThat(SqlFunctions.neAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is(true));

    // Non-numeric different type inequality check
    assertThat(SqlFunctions.neAny("2", 2), is(true));
  }

  @Test public void testLtWithAny() {
    // Non-numeric same type "less then" check
    assertThat(SqlFunctions.ltAny("apple", "banana"), is(true));

    // Numeric types "less than" check
    assertThat(SqlFunctions.ltAny(1, 2L), is(true));
    assertThat(SqlFunctions.ltAny(1, 2.0D), is(true));
    assertThat(SqlFunctions.ltAny(1L, 2.0D), is(true));
    assertThat(SqlFunctions.ltAny(new BigDecimal(1L), 2), is(true));
    assertThat(SqlFunctions.ltAny(new BigDecimal(1L), 2L), is(true));
    assertThat(SqlFunctions.ltAny(new BigDecimal(1L), 2.0D), is(true));
    assertThat(SqlFunctions.ltAny(new BigDecimal(1L), new BigDecimal(2.0D)),
        is(true));

    // Non-numeric different type but both implements Comparable
    // "less than" check
    try {
      assertThat(SqlFunctions.ltAny("1", 2L), is(false));
      fail("'lt' on non-numeric different type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for comparison: class java.lang.String < "
              + "class java.lang.Long"));
    }
  }

  @Test public void testLeWithAny() {
    // Non-numeric same type "less or equal" check
    assertThat(SqlFunctions.leAny("apple", "banana"), is(true));
    assertThat(SqlFunctions.leAny("apple", "apple"), is(true));

    // Numeric types "less or equal" check
    assertThat(SqlFunctions.leAny(1, 2L), is(true));
    assertThat(SqlFunctions.leAny(1, 1L), is(true));
    assertThat(SqlFunctions.leAny(1, 2.0D), is(true));
    assertThat(SqlFunctions.leAny(1, 1.0D), is(true));
    assertThat(SqlFunctions.leAny(1L, 2.0D), is(true));
    assertThat(SqlFunctions.leAny(1L, 1.0D), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 2), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 1), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 2L), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 1L), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 2.0D), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), 1.0D), is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), new BigDecimal(2.0D)),
        is(true));
    assertThat(SqlFunctions.leAny(new BigDecimal(1L), new BigDecimal(1.0D)),
        is(true));

    // Non-numeric different type but both implements Comparable
    // "less or equal" check
    try {
      assertThat(SqlFunctions.leAny("2", 2L), is(false));
      fail("'le' on non-numeric different type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for comparison: class java.lang.String <= "
              + "class java.lang.Long"));
    }
  }

  @Test public void testGtWithAny() {
    // Non-numeric same type "greater then" check
    assertThat(SqlFunctions.gtAny("banana", "apple"), is(true));

    // Numeric types "greater than" check
    assertThat(SqlFunctions.gtAny(2, 1L), is(true));
    assertThat(SqlFunctions.gtAny(2, 1.0D), is(true));
    assertThat(SqlFunctions.gtAny(2L, 1.0D), is(true));
    assertThat(SqlFunctions.gtAny(new BigDecimal(2L), 1), is(true));
    assertThat(SqlFunctions.gtAny(new BigDecimal(2L), 1L), is(true));
    assertThat(SqlFunctions.gtAny(new BigDecimal(2L), 1.0D), is(true));
    assertThat(SqlFunctions.gtAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is(true));

    // Non-numeric different type but both implements Comparable
    // "greater than" check
    try {
      assertThat(SqlFunctions.gtAny("2", 1L), is(false));
      fail("'gt' on non-numeric different type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for comparison: class java.lang.String > "
              + "class java.lang.Long"));
    }
  }

  @Test public void testGeWithAny() {
    // Non-numeric same type "greater or equal" check
    assertThat(SqlFunctions.geAny("banana", "apple"), is(true));
    assertThat(SqlFunctions.geAny("apple", "apple"), is(true));

    // Numeric types "greater or equal" check
    assertThat(SqlFunctions.geAny(2, 1L), is(true));
    assertThat(SqlFunctions.geAny(1, 1L), is(true));
    assertThat(SqlFunctions.geAny(2, 1.0D), is(true));
    assertThat(SqlFunctions.geAny(1, 1.0D), is(true));
    assertThat(SqlFunctions.geAny(2L, 1.0D), is(true));
    assertThat(SqlFunctions.geAny(1L, 1.0D), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(2L), 1), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(1L), 1), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(2L), 1L), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(1L), 1L), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(2L), 1.0D), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(1L), 1.0D), is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is(true));
    assertThat(SqlFunctions.geAny(new BigDecimal(1L), new BigDecimal(1.0D)),
        is(true));

    // Non-numeric different type but both implements Comparable
    // "greater or equal" check
    try {
      assertThat(SqlFunctions.geAny("2", 2L), is(false));
      fail("'ge' on non-numeric different type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for comparison: class java.lang.String >= "
              + "class java.lang.Long"));
    }
  }

  @Test public void testPlusAny() {
    // null parameters
    assertThat(SqlFunctions.plusAny(null, null), nullValue());
    assertThat(SqlFunctions.plusAny(null, 1), nullValue());
    assertThat(SqlFunctions.plusAny(1, null), nullValue());

    // Numeric types
    assertThat(SqlFunctions.plusAny(2, 1L), is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(2, 1.0D), is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(2L, 1.0D), is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(new BigDecimal(2L), 1),
        is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(new BigDecimal(2L), 1L),
        is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(new BigDecimal(2L), 1.0D),
        is((Object) new BigDecimal(3)));
    assertThat(SqlFunctions.plusAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is((Object) new BigDecimal(3)));

    // Non-numeric type
    try {
      SqlFunctions.plusAny("2", 2L);
      fail("'plus' on non-numeric type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for arithmetic: class java.lang.String + "
              + "class java.lang.Long"));
    }
  }

  @Test public void testMinusAny() {
    // null parameters
    assertThat(SqlFunctions.minusAny(null, null), nullValue());
    assertThat(SqlFunctions.minusAny(null, 1), nullValue());
    assertThat(SqlFunctions.minusAny(1, null), nullValue());

    // Numeric types
    assertThat(SqlFunctions.minusAny(2, 1L), is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(2, 1.0D), is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(2L, 1.0D), is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(new BigDecimal(2L), 1),
        is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(new BigDecimal(2L), 1L),
        is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(new BigDecimal(2L), 1.0D),
        is((Object) new BigDecimal(1)));
    assertThat(SqlFunctions.minusAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is((Object) new BigDecimal(1)));

    // Non-numeric type
    try {
      SqlFunctions.minusAny("2", 2L);
      fail("'minus' on non-numeric type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for arithmetic: class java.lang.String - "
              + "class java.lang.Long"));
    }
  }

  @Test public void testMultiplyAny() {
    // null parameters
    assertThat(SqlFunctions.multiplyAny(null, null), nullValue());
    assertThat(SqlFunctions.multiplyAny(null, 1), nullValue());
    assertThat(SqlFunctions.multiplyAny(1, null), nullValue());

    // Numeric types
    assertThat(SqlFunctions.multiplyAny(2, 1L), is((Object) new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(2, 1.0D),
        is((Object) new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(2L, 1.0D),
        is((Object) new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(new BigDecimal(2L), 1),
        is((Object) new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(new BigDecimal(2L), 1L),
        is((Object) new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(new BigDecimal(2L), 1.0D),
        is((Object) new BigDecimal(2)));
    assertThat(SqlFunctions.multiplyAny(new BigDecimal(2L), new BigDecimal(1.0D)),
        is((Object) new BigDecimal(2)));

    // Non-numeric type
    try {
      SqlFunctions.multiplyAny("2", 2L);
      fail("'multiply' on non-numeric type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for arithmetic: class java.lang.String * "
              + "class java.lang.Long"));
    }
  }

  @Test public void testDivideAny() {
    // null parameters
    assertThat(SqlFunctions.divideAny(null, null), nullValue());
    assertThat(SqlFunctions.divideAny(null, 1), nullValue());
    assertThat(SqlFunctions.divideAny(1, null), nullValue());

    // Numeric types
    assertThat(SqlFunctions.divideAny(5, 2L),
        is((Object) new BigDecimal("2.5")));
    assertThat(SqlFunctions.divideAny(5, 2.0D),
        is((Object) new BigDecimal("2.5")));
    assertThat(SqlFunctions.divideAny(5L, 2.0D),
        is((Object) new BigDecimal("2.5")));
    assertThat(SqlFunctions.divideAny(new BigDecimal(5L), 2),
        is((Object) new BigDecimal(2.5)));
    assertThat(SqlFunctions.divideAny(new BigDecimal(5L), 2L),
        is((Object) new BigDecimal(2.5)));
    assertThat(SqlFunctions.divideAny(new BigDecimal(5L), 2.0D),
        is((Object) new BigDecimal(2.5)));
    assertThat(SqlFunctions.divideAny(new BigDecimal(5L), new BigDecimal(2.0D)),
        is((Object) new BigDecimal(2.5)));

    // Non-numeric type
    try {
      SqlFunctions.divideAny("5", 2L);
      fail("'divide' on non-numeric type is not possible");
    } catch (CalciteException e) {
      assertThat(e.getMessage(),
          is("Invalid types for arithmetic: class java.lang.String / "
              + "class java.lang.Long"));
    }
  }

  @Test public void testMultiset() {
    final List<String> abacee = Arrays.asList("a", "b", "a", "c", "e", "e");
    final List<String> adaa = Arrays.asList("a", "d", "a", "a");
    final List<String> addc = Arrays.asList("a", "d", "c", "d", "c");
    final List<String> z = Collections.emptyList();
    assertThat(SqlFunctions.multisetExceptAll(abacee, addc),
        is(Arrays.asList("b", "a", "e", "e")));
    assertThat(SqlFunctions.multisetExceptAll(abacee, z), is(abacee));
    assertThat(SqlFunctions.multisetExceptAll(z, z), is(z));
    assertThat(SqlFunctions.multisetExceptAll(z, addc), is(z));

    assertThat(SqlFunctions.multisetExceptDistinct(abacee, addc),
        is(Arrays.asList("b", "e")));
    assertThat(SqlFunctions.multisetExceptDistinct(abacee, z),
        is(Arrays.asList("a", "b", "c", "e")));
    assertThat(SqlFunctions.multisetExceptDistinct(z, z), is(z));
    assertThat(SqlFunctions.multisetExceptDistinct(z, addc), is(z));

    assertThat(SqlFunctions.multisetIntersectAll(abacee, addc),
        is(Arrays.asList("a", "c")));
    assertThat(SqlFunctions.multisetIntersectAll(abacee, adaa),
        is(Arrays.asList("a", "a")));
    assertThat(SqlFunctions.multisetIntersectAll(adaa, abacee),
        is(Arrays.asList("a", "a")));
    assertThat(SqlFunctions.multisetIntersectAll(abacee, z), is(z));
    assertThat(SqlFunctions.multisetIntersectAll(z, z), is(z));
    assertThat(SqlFunctions.multisetIntersectAll(z, addc), is(z));

    assertThat(SqlFunctions.multisetIntersectDistinct(abacee, addc),
        is(Arrays.asList("a", "c")));
    assertThat(SqlFunctions.multisetIntersectDistinct(abacee, adaa),
        is(Collections.singletonList("a")));
    assertThat(SqlFunctions.multisetIntersectDistinct(adaa, abacee),
        is(Collections.singletonList("a")));
    assertThat(SqlFunctions.multisetIntersectDistinct(abacee, z), is(z));
    assertThat(SqlFunctions.multisetIntersectDistinct(z, z), is(z));
    assertThat(SqlFunctions.multisetIntersectDistinct(z, addc), is(z));

    assertThat(SqlFunctions.multisetUnionAll(abacee, addc),
        is(Arrays.asList("a", "b", "a", "c", "e", "e", "a", "d", "c", "d", "c")));
    assertThat(SqlFunctions.multisetUnionAll(abacee, z), is(abacee));
    assertThat(SqlFunctions.multisetUnionAll(z, z), is(z));
    assertThat(SqlFunctions.multisetUnionAll(z, addc), is(addc));

    assertThat(SqlFunctions.multisetUnionDistinct(abacee, addc),
        is(Arrays.asList("a", "b", "c", "d", "e")));
    assertThat(SqlFunctions.multisetUnionDistinct(abacee, z),
        is(Arrays.asList("a", "b", "c", "e")));
    assertThat(SqlFunctions.multisetUnionDistinct(z, z), is(z));
    assertThat(SqlFunctions.multisetUnionDistinct(z, addc),
        is(Arrays.asList("a", "c", "d")));
  }

  @Test public void testMd5() {
    assertThat("d41d8cd98f00b204e9800998ecf8427e", is(md5("")));
    assertThat("d41d8cd98f00b204e9800998ecf8427e", is(md5(ByteString.of("", 16))));
    assertThat("902fbdd2b1df0c4f70b4a5d23525e932", is(md5("ABC")));
    assertThat("902fbdd2b1df0c4f70b4a5d23525e932",
        is(md5(new ByteString("ABC".getBytes(UTF_8)))));
    try {
      String o = md5((String) null);
      fail("Expected NPE, got " + o);
    } catch (NullPointerException e) {
      // ok
    }
  }

  @Test public void testSha1() {
    assertThat("da39a3ee5e6b4b0d3255bfef95601890afd80709", is(sha1("")));
    assertThat("da39a3ee5e6b4b0d3255bfef95601890afd80709", is(sha1(ByteString.of("", 16))));
    assertThat("3c01bdbb26f358bab27f267924aa2c9a03fcfdb8", is(sha1("ABC")));
    assertThat("3c01bdbb26f358bab27f267924aa2c9a03fcfdb8",
        is(sha1(new ByteString("ABC".getBytes(UTF_8)))));
    try {
      String o = sha1((String) null);
      fail("Expected NPE, got " + o);
    } catch (NullPointerException e) {
      // ok
    }
  }

  /** Test for {@link SqlFunctions#nvl}. */
  @Test public void testNvl() {
    assertThat(nvl("a", "b"), is("a"));
    assertThat(nvl(null, "b"), is("b"));
    assertThat(nvl(null, null), nullValue());
    assertThat(nvl(1, 1), is(1));
    assertThat(nvl(substring("abc", 1, 1), "b"), is("a"));
  }

  /** Test for {@link SqlFunctions#ifNull}. */
  @Test public void testifNull() {
    assertThat(ifNull("a", "b"), is("a"));
    assertThat(ifNull(null, "b"), is("b"));
    assertThat(ifNull(null, null), nullValue());
    assertThat(ifNull(1, 1), is(1));
    assertThat(ifNull(substring("abc", 1, 1), "b"), is("a"));
  }

  /** Test for {@link SqlFunctions#isNull}. */
  @Test public void testisNull() {
    assertThat(isNull("a", "b"), is("a"));
    assertThat(isNull(null, "b"), is("b"));
    assertThat(isNull(null, null), nullValue());
    assertThat(isNull(1, 1), is(1));
    assertThat(isNull(substring("abc", 1, 1), "b"), is("a"));
  }

  /** Test for {@link SqlFunctions#lpad}. */
  @Test public void testLPAD() {
    assertThat(lpad("123", 6, "%"), is("%%%123"));
    assertThat(lpad("123", 6), is("   123"));
    assertThat(lpad("123", 6, "456"), is("456123"));
    assertThat(lpad("pilot", 4, "auto"), is("pilo"));
    assertThat(lpad("pilot", 9, "auto"), is("autopilot"));
  }

  /** Test for {@link SqlFunctions#rpad}. */
  @Test public void testRPAD() {
    assertThat(rpad("123", 6, "%"), is("123%%%"));
    assertThat(rpad("123", 6), is("123   "));
    assertThat(rpad("123", 6, "456"), is("123456"));
    assertThat(rpad("pilot", 4, "auto"), is("pilo"));
    assertThat(rpad("auto", 9, "pilot"), is("autopilot"));
  }

  /** Test for {@link SqlFunctions#format}. */
  @Test public void testFormat() {
    assertThat(format("%4d", 23), is("  23"));
    assertThat(format("%4.1f", 1.5), is(" 1.5"));
    assertThat(format("%1.14E", 177.5879), is("1.77587900000000E+02"));
    assertThat(format("%05d", 1879), is("01879"));
  }

  /** Test for {@link SqlFunctions#toVarchar}. */
  @Test public void testToVarchar() {
    assertThat(toVarchar(null, null), nullValue());
    assertThat(toVarchar(23, "99"), is("23"));
    assertThat(toVarchar(123, "999"), is("123"));
    assertThat(toVarchar(1.5, "9.99"), is("1.50"));
  }

  /** Test for {@link SqlFunctions#weekNumberOfYear}. */
  @Test public void testWeekNumberofYear() {
    assertThat(weekNumberOfYear("2019-03-12"), is(15));
    assertThat(weekNumberOfYear("2019-07-12"), is(33));
    assertThat(weekNumberOfYear("2019-09-12"), is(41));
  }

  /** Test for {@link SqlFunctions#yearNumberOfCalendar}. */
  @Test public void testYearNumberOfCalendar() {
    assertThat(yearNumberOfCalendar("2019-03-12"), is(2019));
    assertThat(yearNumberOfCalendar("1901-07-01"), is(1901));
    assertThat(yearNumberOfCalendar("1900-12-28"), is(1900));
  }

  /** Test for {@link SqlFunctions#monthNumberOfYear}. */
  @Test public void testMonthNumberOfYear() {
    assertThat(monthNumberOfYear("2019-03-12"), is(3));
    assertThat(monthNumberOfYear("1901-07-01"), is(7));
    assertThat(monthNumberOfYear("1900-12-28"), is(12));
  }

  /** Test for {@link SqlFunctions#quarterNumberOfYear}. */
  @Test public void testQuarterNumberOfYear() {
    assertThat(quarterNumberOfYear("2019-03-12"), is(1));
    assertThat(quarterNumberOfYear("1901-07-01"), is(3));
    assertThat(quarterNumberOfYear("1900-12-28"), is(4));
  }

  /** Test for {@link SqlFunctions#monthNumberOfQuarter}. */
  @Test public void testMonthNumberOfQuarter() {
    assertThat(monthNumberOfQuarter("2019-03-12"), is(3));
    assertThat(monthNumberOfQuarter("1901-07-01"), is(1));
    assertThat(monthNumberOfQuarter("1900-09-28"), is(3));
  }

  /** Test for {@link SqlFunctions#weekNumberOfMonth}. */
  @Test public void testWeekNumberOfMonth() {
    assertThat(weekNumberOfMonth("2019-03-12"), is(1));
    assertThat(weekNumberOfMonth("1901-07-01"), is(0));
    assertThat(weekNumberOfMonth("1900-09-28"), is(4));
  }

  /** Test for {@link SqlFunctions#dayOccurrenceOfMonth}. */
  @Test public void testDayOccurrenceOfMonth() {
    assertThat(dayOccurrenceOfMonth("2019-03-12"), is(2));
    assertThat(dayOccurrenceOfMonth("2019-07-15"), is(3));
    assertThat(dayOccurrenceOfMonth("2019-09-20"), is(3));
  }

  /** Test for {@link SqlFunctions#weekNumberOfCalendar}. */
  @Test public void testWeekNumberOfCalendar() {
    assertThat(weekNumberOfCalendar("2019-03-12"), is(6198));
    assertThat(weekNumberOfCalendar("1901-07-01"), is(78));
    assertThat(weekNumberOfCalendar("1900-09-01"), is(35));
  }

  /** Test for {@link SqlFunctions#dayNumberOfCalendar}. */
  @Test public void testDayNumberOfCalendar() {
    assertThat(dayNumberOfCalendar("2019-03-12"), is(43535));
    assertThat(dayNumberOfCalendar("1901-07-01"), is(547));
    assertThat(dayNumberOfCalendar("1900-09-01"), is(244));
  }

  /** Test for {@link SqlFunctions#dateMod}. */
  @Test public void testDateMod() {
    assertThat(dateMod("2019-03-12", 1023), is(1190300));
    assertThat(dateMod("2008-07-15", 5794), is(1080700));
    assertThat(dateMod("2014-01-27", 8907), is(1140100));
  }

  /** Test for {@link SqlFunctions#timestampToDate}. */
  @Test public void testTimestampToDate() {
    assertThat(timestampToDate("2020-12-12 12:12:12").toString(), is("2020-12-12"));
    assertThat(timestampToDate(new Timestamp(1607731932)).toString(), is("1970-01-19"));
  }

  /** Test for {@link SqlFunctions#instr}. */
  @Test public void testInStr() {
    assertThat(instr("Choose a chocolate chip cookie", "ch", 2, 2), is(20));
    assertThat(instr("Choose a chocolate chip cookie", "cc", 2, 2), is(0));
  }

  /** Test for {@link SqlFunctions#charindex}. */
  @Test public void testCharindex() {
    assertThat(charindex("xy", "Choose a chocolate chip cookie", 2), is(0));
    assertThat(charindex("ch", "Choose a chocolate chip cookie", 1), is(1));
    assertThat(charindex("ch", "Choose a chocolate chip cookie", 2), is(10));
  }

  /** Test for {@link SqlFunctions#datetimeAdd(Object, Object)}. */
  @Test public void testdatetimeAdd() {
    assertThat(datetimeAdd("2000-12-12 12:12:12", "INTERVAL 1 DAY"),
        is(Timestamp.valueOf("2000-12-13 12:12:12.0")));
  }

  /** Test for {@link SqlFunctions#datetimeSub(Object, Object)}. */
  @Test public void testdatetimeSub() {
    assertThat(datetimeSub("2000-12-12 12:12:12", "INTERVAL 1 DAY"),
        is(Timestamp.valueOf("2000-12-11 12:12:12.0")));
  }

  /** Test for {@link SqlFunctions#toBinary(Object, Object)}. */
  @Test public void testToBinary() {
    assertThat(toBinary("williams", "UTF-8"), is("77696C6C69616D73"));
    assertThat(toBinary("david", "UTF-8"), is("6461766964"));
  }

  /** Test for {@link SqlFunctions#timeSub(Object, Object)}. */
  @Test public void testTimeSub() {
    assertThat(timeSub("15:30:00", "INTERVAL 10 MINUTE"), is(Time.valueOf("15:20:00")));
    assertThat(timeSub("10:00:00", "INTERVAL 1 HOUR"), is(Time.valueOf("09:00:00")));
  }

  /** Test for {@link SqlFunctions#toCharFunction(Object, Object)}. */
  @Test public void testToChar() {
    assertThat(toCharFunction(null, null), nullValue());
    assertThat(toCharFunction(23, "99"), is("23"));
    assertThat(toCharFunction(123, "999"), is("123"));
    assertThat(toCharFunction(1.5, "9.99"), is("1.50"));
  }

  @Test public void monthsBetween() {
    assertThat(SqlFunctions.monthsBetween("2020-05-23", "2020-04-23"), is(1.0));
    assertThat(SqlFunctions.monthsBetween("2020-05-26", "2020-04-20"), is(1.193548387));
    assertThat(SqlFunctions.monthsBetween("2019-05-26", "2020-04-20"), is(-10.806451613));
  }

  @Test public void cotFunctionTest() {
    assertThat(cotFunction(0.12), is(8.293294880594532));
  }

  @Test public void bitwiseAndFunctionTest() {
    assertThat(bitwiseAnd(3, 6), is(2));
  }

  @Test public void bitwiseORFunctionTest() {
    assertThat(bitwiseOR(3, 6), is(7));
  }

  @Test public void bitwiseXORFunctionTest() {
    assertThat(bitwiseXOR(3, 6), is(5));
  }

  @Test public void bitwiseSHRFunctionTest() {
    assertThat(bitwiseSHR(3, 1, 6), is(1));
  }

  @Test public void bitwiseSHLFunctionTest() {
    assertThat(bitwiseSHL(3, 1, 6), is(4));
  }

  @Test public void piTest() {
    assertThat(SqlFunctions.pi(), is(3.141592653589793));
  }

  @Test public void testOctetLengthWithLiteral() {
    assertThat(octetLength("abc"), is(3));
  }

  /** Test for {@link SqlFunctions#strTok(Object, Object, Object)}. */
  @Test public void testStrtok() {
    assertThat(strTok("abcd-def-ghi", "-", 1), is("abcd"));
    assertThat(strTok("a.b.c.d", "\\.", 3), is("c"));
  }

  /** Test for {@link SqlFunctions#toCharFunction(Object, Object)}. */
  @Test public void testDateTimeForm() {
    assertThat(toCharFunction(111200, "HHMISS"), is("111200"));
  }

  /** Test for {@link SqlFunctions#regexpMatchCount(Object, Object, Object, Object)}. */
  @Test public void testRegexpMatchCount() {
    String regex = "Ste(v|ph)en";
    assertThat(
        regexpMatchCount("Steven Jones and Stephen Smith are the best players",
        regex, 0, ""), is(2));
    String bestPlayers = "Steven Jones and Stephen are the best players";
    assertThat(
        regexpMatchCount(bestPlayers,
         "Jon", 5, "i"), is(1));
    assertThat(
        regexpMatchCount(bestPlayers,
        "Jon", 20, "i"), is(0));
  }

  /** Test for {@link SqlFunctions#regexpContains(Object, Object)}. */
  @Test public void testRegexpContains() {
    assertThat(regexpContains("foo@example.com", "@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+"), is(true));
    assertThat(regexpContains("www.example.net", "@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+"), is(false));
  }

  /** Test for {@link SqlFunctions#regexpExtract(Object, Object, Object, Object)}. */
  @Test public void testRegexpExtract() {
    assertThat(regexpExtract("foo@example.com", "^[a-zA-Z0-9_.+-]+", 0, 0),
        is("foo"));
    assertThat(regexpExtract("cat on the mat", ".at", 0, 0),
        is("cat"));
    assertThat(regexpExtract("cat on the mat", ".at", 0, 1),
        is("mat"));
  }
}

// End SqlFunctionsTest.java
