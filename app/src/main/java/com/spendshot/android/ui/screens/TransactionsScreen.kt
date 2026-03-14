package com.spendshot.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spendshot.android.data.SettingsEntity
import com.spendshot.android.data.TransactionEntity
import com.spendshot.android.ui.components.TransactionItem
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>?,
    settings: SettingsEntity?,
    selectedTransactionIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    categoryIcons: Map<String, String?> = emptyMap()
) {
    if (transactions == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }
    val isInSelectionMode = selectedTransactionIds.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp), // To offset for the bottom nav bar
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions for this month.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val groupedTransactions = transactions.groupBy { txn ->
                SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(txn.timestamp)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp)
            ) {
                groupedTransactions.forEach { (date, txnsForDate) ->
                    stickyHeader {
                        val transactionCount = txnsForDate.size
                        val headerText = when (transactionCount) {
                            1 -> "$date ($transactionCount transaction)"
                            else -> "$date ($transactionCount transactions)"
                        }

                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    items(txnsForDate, key = { it.id }) { txn ->
                        val isSelected = selectedTransactionIds.contains(txn.id)
                        TransactionItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .animateItem()
                                .combinedClickable(
                                    onClick = {
                                        if (isInSelectionMode) {
                                            onSelectionChange(
                                                if (isSelected) {
                                                    selectedTransactionIds - txn.id
                                                } else {
                                                    selectedTransactionIds + txn.id
                                                }
                                            )
                                        } else {
                                            onTransactionClick(txn)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isInSelectionMode) {
                                            onSelectionChange(selectedTransactionIds + txn.id)
                                        }
                                    }
                                ),
                            transaction = txn,
                            isSelected = isSelected,
                            categoryIconUrl = categoryIcons[txn.category]
                        )
                    }
                }
            }
        }
    }
}
