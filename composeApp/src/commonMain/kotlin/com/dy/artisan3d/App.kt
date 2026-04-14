package com.dy.artisan3d

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.dy.artisan3d.data.AppLanguage
import com.dy.artisan3d.domain.Localization
import com.dy.artisan3d.navigation.Navigator
import com.dy.artisan3d.navigation.Screen
import com.dy.artisan3d.ui.screen.choose_language.ChooseLanguage
import com.dy.artisan3d.ui.screen.main.MainScreen
import com.dy.artisan3d.ui.screen.main.article.detail.ArtisanArticleDetailScreen
import com.dy.artisan3d.ui.screen.main.article.detail.ArtisanArticleDetailViewModel
import com.dy.artisan3d.ui.screen.main.settings.Settings
import com.dy.artisan3d.ui.screen.on_boarding.OnboardingScreen
import com.dy.artisan3d.ui.screen.products.list.ProductListContent
import com.dy.artisan3d.ui.screen.products.list.ProductListViewModel
import com.dy.artisan3d.ui.screen.products.view.ProductDetailScreen
import com.dy.artisan3d.ui.screen.products.view.ProductDetailViewModel
import com.dy.artisan3d.ui.theme.Artisan3DTheme
import dev.burnoo.compose.remembersetting.rememberBooleanSetting
import dev.burnoo.compose.remembersetting.rememberStringSetting
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf


@OptIn(KoinExperimentalAPI::class, ExperimentalComposeUiApi::class)
@Composable
fun App() {

    val localization = koinInject<Localization>()
    val isAndroid = isAndroidPlatform()


    var languageISO by rememberStringSetting(
        key = "languageISO",
        defaultValue = "en"
    )

    val appLang = koinInject<AppLanguage>()
    appLang.current = languageISO

    localization.applyLanguage(languageISO)

    var savedStartDestination by rememberStringSetting(
        key = "startDestination",
        defaultValue = "language"
    )

    var savedDarkMode by rememberBooleanSetting(
        key = "darkMode",
        defaultValue = isSystemInDarkTheme()
    )

    val startDestination = remember {
        if (savedStartDestination == "language") Screen.ChooseLanguage else Screen.Main
    }

    // MUHIM: rememberSaveable endi backStack-ni o'lmaydigan qiladi
    val navigator = rememberSaveable(saver = Navigator.saver(startDestination)) {
        Navigator(startDestination)
    }

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }


    Artisan3DTheme(darkTheme = savedDarkMode) {

        val backgroundColor by animateColorAsState(
            targetValue = MaterialTheme.colorScheme.background,
            animationSpec = tween(durationMillis = 300),
            label = "bgColor"
        )


        val onBackgroundColor by animateColorAsState(
            targetValue = MaterialTheme.colorScheme.onBackground,
            animationSpec = tween(durationMillis = 300),
            label = "onBgColor"
        )

        Surface(
            modifier = Modifier.fillMaxSize()
                .background(backgroundColor)
                .then(
                    if (isAndroid) Modifier.systemBarsPadding() else Modifier
                ),
            //Shu kerak
            color = backgroundColor, // Dark/Light-ga qarab o'zgaradi
            contentColor = onBackgroundColor // Matn rangini belgilaydi
        ) {

            NavDisplay(
                backStack = navigator.backStack,
                onBack = { navigator.goBack() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = entryProvider {
                    entry<Screen.ChooseLanguage> {
                        ChooseLanguage(
                            lang = languageISO,
                            onChangeLanguage = {
                                languageISO = it

                            }) {
                            navigator.navigateTo(Screen.OnBoarding)
                        }
                    }

                    entry<Screen.OnBoarding> {
                        OnboardingScreen(onFinished = {
                            //savedStartDestination = "2"
                            //navigator.replaceStack(Screen.Main)
                            navigator.navigateTo(Screen.Main)
                        })
                    }

                    entry<Screen.Main> {
                        MainScreen(
                            onNextScreen = {
                                navigator.navigateTo(it)
                            }
                        )
                    }

                    entry<Screen.Products> { entry ->
                        val productId = entry.categoryId
                        val brandId = entry.brandId
                        val title = entry.title
                        val viewModel = koinViewModel<ProductListViewModel>(
                            key = "$productId-$title"
                        ) {
                            parametersOf(productId, brandId, title)
                        }

                        ProductListContent(
                            viewModel,
                            onProductClick = {
                                navigator.navigateTo(Screen.ProductDetail(it))
                            }, onBack = {
                                navigator.goBack()
                            })
                    }

                    entry<Screen.ProductDetail> { entry ->
                        val productId = entry.productId
                        val viewModel = koinViewModel<ProductDetailViewModel>(
                            key = "product-view-$productId"
                        ) {
                            parametersOf(productId)
                        }

                        ProductDetailScreen(viewModel) {
                            navigator.goBack()
                        }
                    }

                    entry<Screen.ArticleDetail> { entry ->
                        val articleId = entry.articleId

                        val viewModel = koinViewModel<ArtisanArticleDetailViewModel>(
                            key = "article-detail-$articleId"
                        ) {
                            parametersOf(articleId)
                        }

                        ArtisanArticleDetailScreen(viewModel) {
                            navigator.goBack()

                        }
                    }

                    entry<Screen.Settings> {
                        Settings(
                            darkMode = savedDarkMode,
                            onDarkModeChange = {
                                savedDarkMode = it
                            }
                        )
                    }
                },
            )
        }
    }
}