package dev.abhinav.tenniscalendar.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import dev.abhinav.tenniscalendar.model.Tournament
import dev.abhinav.tenniscalendar.ui.theme.TennisTokens

/**
 * Renders a tournament logo from `assets/logos/<file>` via Coil, falling back to the tier-colored
 * abbreviation badge whenever the asset is missing — matching the web `TournamentLogo`.
 */
@Composable
fun TournamentBadge(tournament: Tournament, size: Int, modifier: Modifier = Modifier) {
    val context = LocalPlatformContext.current
    val request = ImageRequest.Builder(context)
        .data("file:///android_asset/logos/${tournament.logo}")
        .build()

    SubcomposeAsyncImage(
        model = request,
        contentDescription = tournament.name,
        modifier = modifier.size(size.dp),
        loading = { FallbackBadge(tournament, size) },
        error = { FallbackBadge(tournament, size) },
    )
}

@Composable
fun FallbackBadge(tournament: Tournament, size: Int, modifier: Modifier = Modifier) {
    val color = TennisTokens.tierColor(tournament.level, isSystemInDarkTheme())
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.22f).dp))
            .background(color.copy(alpha = 0.18f))
            .border((size * 0.03f).coerceAtLeast(1f).dp, color.copy(alpha = 0.7f), RoundedCornerShape((size * 0.22f).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = TennisTokens.abbreviation(tournament.name),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.34f).sp,
            textAlign = TextAlign.Center,
            lineHeight = (size * 0.34f).sp,
        )
    }
}

/**
 * Player-nationality code → flag emoji. The data uses **IOC** codes (e.g. GER, SUI, NED, MON),
 * NOT ISO 3166-1 alpha-3 — so this maps IOC → ISO 3166-1 alpha-2, then to regional-indicator
 * emoji. Unknown codes fall back to the raw code; empty/blank codes render nothing.
 */
fun countryFlag(iocCode: String): String {
    val code = iocCode.trim()
    if (code.isEmpty()) return ""
    val alpha2 = IOC_TO_ISO2[code.uppercase()] ?: return code
    return alpha2.map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }.joinToString("")
}

// Comprehensive IOC (NOC) → ISO 3166-1 alpha-2 table. Covers every current NOC, so any nationality
// that appears in the tour data resolves to a flag rather than the raw-code fallback.
private val IOC_TO_ISO2 = mapOf(
    "AFG" to "AF", "ALB" to "AL", "ALG" to "DZ", "AND" to "AD", "ANG" to "AO", "ANT" to "AG",
    "ARG" to "AR", "ARM" to "AM", "ARU" to "AW", "ASA" to "AS", "AUS" to "AU", "AUT" to "AT",
    "AZE" to "AZ", "BAH" to "BS", "BAN" to "BD", "BAR" to "BB", "BDI" to "BI", "BEL" to "BE",
    "BEN" to "BJ", "BER" to "BM", "BHU" to "BT", "BIH" to "BA", "BIZ" to "BZ", "BLR" to "BY",
    "BOL" to "BO", "BOT" to "BW", "BRA" to "BR", "BRN" to "BH", "BRU" to "BN", "BUL" to "BG",
    "BUR" to "BF", "CAF" to "CF", "CAM" to "KH", "CAN" to "CA", "CAY" to "KY", "CGO" to "CG",
    "CHA" to "TD", "CHI" to "CL", "CHN" to "CN", "CIV" to "CI", "CMR" to "CM", "COD" to "CD",
    "COK" to "CK", "COL" to "CO", "COM" to "KM", "CPV" to "CV", "CRC" to "CR", "CRO" to "HR",
    "CUB" to "CU", "CYP" to "CY", "CZE" to "CZ", "DEN" to "DK", "DJI" to "DJ", "DMA" to "DM",
    "DOM" to "DO", "ECU" to "EC", "EGY" to "EG", "ERI" to "ER", "ESA" to "SV", "ESP" to "ES",
    "EST" to "EE", "ETH" to "ET", "FIJ" to "FJ", "FIN" to "FI", "FRA" to "FR", "FSM" to "FM",
    "GAB" to "GA", "GAM" to "GM", "GBR" to "GB", "GBS" to "GW", "GEO" to "GE", "GEQ" to "GQ",
    "GER" to "DE", "GHA" to "GH", "GRE" to "GR", "GRN" to "GD", "GUA" to "GT", "GUI" to "GN",
    "GUM" to "GU", "GUY" to "GY", "HAI" to "HT", "HKG" to "HK", "HON" to "HN", "HUN" to "HU",
    "INA" to "ID", "IND" to "IN", "IRI" to "IR", "IRL" to "IE", "IRQ" to "IQ", "ISL" to "IS",
    "ISR" to "IL", "ISV" to "VI", "ITA" to "IT", "IVB" to "VG", "JAM" to "JM", "JOR" to "JO",
    "JPN" to "JP", "KAZ" to "KZ", "KEN" to "KE", "KGZ" to "KG", "KIR" to "KI", "KOR" to "KR",
    "KOS" to "XK", "KSA" to "SA", "KUW" to "KW", "LAO" to "LA", "LAT" to "LV", "LBA" to "LY",
    "LBN" to "LB", "LBR" to "LR", "LCA" to "LC", "LES" to "LS", "LIE" to "LI", "LTU" to "LT",
    "LUX" to "LU", "MAD" to "MG", "MAR" to "MA", "MAS" to "MY", "MAW" to "MW", "MDA" to "MD",
    "MDV" to "MV", "MEX" to "MX", "MGL" to "MN", "MHL" to "MH", "MKD" to "MK", "MLI" to "ML",
    "MLT" to "MT", "MNE" to "ME", "MON" to "MC", "MOZ" to "MZ", "MRI" to "MU", "MTN" to "MR",
    "MYA" to "MM", "NAM" to "NA", "NCA" to "NI", "NED" to "NL", "NEP" to "NP", "NGR" to "NG",
    "NIG" to "NE", "NOR" to "NO", "NRU" to "NR", "NZL" to "NZ", "OMA" to "OM", "PAK" to "PK",
    "PAN" to "PA", "PAR" to "PY", "PER" to "PE", "PHI" to "PH", "PLE" to "PS", "PLW" to "PW",
    "PNG" to "PG", "POL" to "PL", "POR" to "PT", "PRK" to "KP", "PUR" to "PR", "QAT" to "QA",
    "ROU" to "RO", "RSA" to "ZA", "RUS" to "RU", "RWA" to "RW", "SAM" to "WS", "SEN" to "SN",
    "SEY" to "SC", "SGP" to "SG", "SKN" to "KN", "SLE" to "SL", "SLO" to "SI", "SMR" to "SM",
    "SOL" to "SB", "SOM" to "SO", "SRB" to "RS", "SRI" to "LK", "SSD" to "SS", "STP" to "ST",
    "SUD" to "SD", "SUI" to "CH", "SUR" to "SR", "SVK" to "SK", "SWE" to "SE", "SWZ" to "SZ",
    "SYR" to "SY", "TAN" to "TZ", "TGA" to "TO", "THA" to "TH", "TJK" to "TJ", "TKM" to "TM",
    "TLS" to "TL", "TOG" to "TG", "TPE" to "TW", "TTO" to "TT", "TUN" to "TN", "TUR" to "TR",
    "TUV" to "TV", "UAE" to "AE", "UGA" to "UG", "UKR" to "UA", "URU" to "UY", "USA" to "US",
    "UZB" to "UZ", "VAN" to "VU", "VEN" to "VE", "VIE" to "VN", "VIN" to "VC", "YEM" to "YE",
    "ZAM" to "ZM", "ZIM" to "ZW",
)
