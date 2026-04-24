package com.nice.cataloguevastra

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.nice.cataloguevastra.ui.activities.LoginActivity
import com.nice.cataloguevastra.databinding.ActivityMainBinding
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNavItems: List<BottomNavItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!(application as CatalogueVastraApp).appContainer.sessionManager.hasToken()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.white)
            ),
            navigationBarStyle = SystemBarStyle.light(
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.white)
            )
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.shellAppBarInclude.root.updatePadding(top = systemBars.top)
            binding.navHostFragment.updatePadding(
                left = systemBars.left,
                right = systemBars.right
            )
            binding.bottomNavigation.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.nav_host_fragment
        ) as NavHostFragment
        val navController = navHostFragment.navController

        setupBottomNavigation(navController)
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            updateBottomNavigationSelection(destination.id, arguments)
            renderCreditsBalance()
        }
        updateBottomNavigationSelection(
            navController.currentDestination?.id ?: R.id.studioFragment,
            navController.currentBackStackEntry?.arguments
        )
        renderCreditsBalance()
    }

    override fun onResume() {
        super.onResume()
        renderCreditsBalance()
    }

    private fun setupBottomNavigation(navController: NavController) {
        bottomNavItems = listOf(
            BottomNavItem(
                binding.studioNavItem,
                binding.studioNavIconContainer,
                binding.studioNavIcon,
                binding.studioNavLabel,
                R.id.studioFragment
            ),
            BottomNavItem(
                binding.cataloguesNavItem,
                binding.cataloguesNavIconContainer,
                binding.cataloguesNavIcon,
                binding.cataloguesNavLabel,
                R.id.cataloguesFragment
            ),
            BottomNavItem(
                binding.assetsNavItem,
                binding.assetsNavIconContainer,
                binding.assetsNavIcon,
                binding.assetsNavLabel,
                R.id.assetsFragment
            ),
            BottomNavItem(
                binding.pricingNavItem,
                binding.pricingNavIconContainer,
                binding.pricingNavIcon,
                binding.pricingNavLabel,
                R.id.pricingFragment
            ),
            BottomNavItem(
                binding.accountNavItem,
                binding.accountNavIconContainer,
                binding.accountNavIcon,
                binding.accountNavLabel,
                R.id.accountFragment
            )
        )

        bottomNavItems.forEach { item ->
            item.root.setOnClickListener {
                if (navController.currentDestination?.id != item.destinationId) {
                    navController.navigate(
                        item.destinationId,
                        null,
                        navOptions {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    )
                }
            }
        }
    }

    private fun updateBottomNavigationSelection(destinationId: Int, arguments: Bundle? = null) {
        val activeDestinationId = when (destinationId) {
            R.id.buyCreditsFragment -> R.id.studioFragment
            R.id.generatedCatalogueFragment -> R.id.cataloguesFragment
            R.id.assetPreviewFragment -> when (arguments?.getString(com.nice.cataloguevastra.ui.fragments.AssetPreviewFragment.ARG_SOURCE)) {
                com.nice.cataloguevastra.ui.fragments.AssetPreviewFragment.SOURCE_CATALOGUES -> R.id.cataloguesFragment
                else -> R.id.assetsFragment
            }
            R.id.updatePasswordFragment -> R.id.accountFragment
            else -> destinationId
        }
        val selectedIconColor = ContextCompat.getColor(this, R.color.white)
        val unselectedIconColor = ContextCompat.getColor(this, R.color.bottomNavInactive)
        val selectedTextColor = ContextCompat.getColor(this, R.color.primaryColor)
        val unselectedTextColor = ContextCompat.getColor(this, R.color.bottomNavInactive)
        val selectedBackground = ResourcesCompat.getDrawable(
            resources,
            R.drawable.gradient_bg,
            theme
        )

        bottomNavItems.forEach { item ->
            val isSelected = item.destinationId == activeDestinationId
            item.iconContainer.background = if (isSelected) selectedBackground?.constantState?.newDrawable()?.mutate() else null
            item.icon.setColorFilter(if (isSelected) selectedIconColor else unselectedIconColor)
            item.label.setTextColor(if (isSelected) selectedTextColor else unselectedTextColor)
        }
    }

    private fun renderCreditsBalance() {
        val balance = (application as CatalogueVastraApp)
            .appContainer
            .sessionManager
            .getCreditsBalance()
        binding.shellAppBarInclude.creditBalanceTv.text =
            NumberFormat.getIntegerInstance().format(balance)
    }

    private data class BottomNavItem(
        val root: View,
        val iconContainer: FrameLayout,
        val icon: ImageView,
        val label: TextView,
        val destinationId: Int
    )
}
