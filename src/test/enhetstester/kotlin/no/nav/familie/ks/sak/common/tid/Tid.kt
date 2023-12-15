import java.time.LocalDate
import java.time.YearMonth

fun jan(år: Int) = YearMonth.of(år, 1)

fun feb(år: Int) = YearMonth.of(år, 2)

fun mars(år: Int) = YearMonth.of(år, 3)

fun apr(år: Int) = YearMonth.of(år, 4)

fun mai(år: Int) = YearMonth.of(år, 5)

fun jun(år: Int) = YearMonth.of(år, 6)

fun jul(år: Int) = YearMonth.of(år, 7)

fun aug(år: Int) = YearMonth.of(år, 8)

fun sep(år: Int) = YearMonth.of(år, 9)

fun okt(år: Int) = YearMonth.of(år, 10)

fun nov(år: Int) = YearMonth.of(år, 11)

fun des(år: Int) = YearMonth.of(år, 12)

fun Int.jan(år: Int) = LocalDate.of(år, 1, this)

fun Int.feb(år: Int) = LocalDate.of(år, 2, this)

fun Int.mars(år: Int) = LocalDate.of(år, 3, this)

fun Int.apr(år: Int) = LocalDate.of(år, 4, this)

fun Int.mai(år: Int) = LocalDate.of(år, 5, this)

fun Int.jun(år: Int) = LocalDate.of(år, 6, this)

fun Int.jul(år: Int) = LocalDate.of(år, 7, this)

fun Int.aug(år: Int) = LocalDate.of(år, 8, this)

fun Int.sep(år: Int) = LocalDate.of(år, 9, this)

fun Int.okt(år: Int) = LocalDate.of(år, 10, this)

fun Int.nov(år: Int) = LocalDate.of(år, 11, this)

fun Int.des(år: Int) = LocalDate.of(år, 12, this)
