package ergonames.Minter
import com.dav009.ergopilot.Simulator._
import com.dav009.ergopilot.model.{TokenAmount, TokenInfo}
import org.ergoplatform.appkit._
import org.ergoplatform.P2PKAddress
import org.scalatest.{PropSpec, Matchers}
import org.ergoplatform.ErgoAddressEncoder
import org.scalatest._
import org.scalatest.{Matchers, WordSpecLike}

class MintNFTSpec extends WordSpecLike with Matchers {

  "emulate a simple transaction" in {

    val (blockchainSim, ergoClient) =
      newBlockChainSimulationScenario("Mint nft test")

    ergoClient.execute((ctx: BlockchainContext) => {

      val minterUser = blockchainSim.newParty("receiver", ctx)
      val initialAmount = 10000000000L
      minterUser.generateUnspentBoxes(toSpend = initialAmount)
      val amountToSpend = Parameters.MinChangeValue + Parameters.MinFee
      val boxes = minterUser.wallet.getUnspentBoxes(amountToSpend).get
      val txBuilder = ctx.newTxBuilder()
      val description = "some token"
      val tokenName = "some name"

      val mintTx = MintToken.ensembleTransaction(
        ctx,
        txBuilder,
        amountToSpend,
        boxes,
        minterUser.wallet.getAddress,
        tokenName,
        description
      )
      ctx.sendTransaction(minterUser.wallet.sign(mintTx))

      val minterOwnedTokens =
        blockchainSim.getUnspentTokensFor(minterUser.wallet.getAddress)
      assert(
        minterOwnedTokens == List(
          new TokenAmount(new TokenInfo(boxes.get(0).getId(), tokenName), 1)
        )
      )

      val minterUnspentCoins =
        blockchainSim.getUnspentCoinsFor(minterUser.wallet.getAddress)
      assert(minterUnspentCoins == (initialAmount - MinTxFee))

    })
  }
}
