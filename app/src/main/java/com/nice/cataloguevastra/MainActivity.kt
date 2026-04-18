package com.nice.cataloguevastra

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
import com.nice.cataloguevastra.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNavItems: List<BottomNavItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateBottomNavigationSelection(destination.id)
        }
        updateBottomNavigationSelection(navController.currentDestination?.id ?: R.id.studioFragment)
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

    private fun updateBottomNavigationSelection(destinationId: Int) {
        val activeDestinationId = when (destinationId) {
            R.id.generatedCatalogueFragment -> R.id.studioFragment
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

    private data class BottomNavItem(
        val root: View,
        val iconContainer: FrameLayout,
        val icon: ImageView,
        val label: TextView,
        val destinationId: Int
    )
}
