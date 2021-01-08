// Copyright (c) Microsoft and contributors.  All rights reserved.
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package dualprotocol.sdk.sample;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.netapp.v2020_06_01.implementation.*;
import dualprotocol.sdk.sample.common.Utils;

import java.util.concurrent.CompletableFuture;

public class Creation
{
    /**
     * Creates an ANF Account
     * @param anfClient Azure NetApp Files Management Client
     * @param accountParams Contains resource group and Account name to use
     * @param accountBody The Account body used in the creation
     * @return The newly created ANF Account
     */
    public static CompletableFuture<NetAppAccountInner> createANFAccount(AzureNetAppFilesManagementClientImpl anfClient, String[] accountParams, NetAppAccountInner accountBody)
    {
        try
        {
            NetAppAccountInner anfAccount = anfClient.accounts().createOrUpdate(accountParams[0], accountParams[1], accountBody);
            Utils.writeSuccessMessage("Account successfully created, resourceId: " + anfAccount.id());

            return CompletableFuture.completedFuture(anfAccount);
        }
        catch (CloudException e)
        {
            Utils.writeConsoleMessage("An error occurred while creating account: " + e.body().message());
            throw e;
        }
    }

    /**
     * Creates a Capacity Pool
     * @param anfClient Azure NetApp Files Management Client
     * @param poolParams Contains resource group, Account name, and Pool name to use
     * @param poolBody The Capacity Pool body used in the creation
     * @return The newly created Capacity Pool
     */
    public static CompletableFuture<CapacityPoolInner> createCapacityPool(AzureNetAppFilesManagementClientImpl anfClient, String[] poolParams, CapacityPoolInner poolBody)
    {
        try
        {
            CapacityPoolInner capacityPool = anfClient.pools().createOrUpdate(poolParams[0], poolParams[1], poolParams[2], poolBody);
            Utils.writeSuccessMessage("Capacity Pool successfully created, resourceId: " + capacityPool.id());

            return CompletableFuture.completedFuture(capacityPool);
        }
        catch (CloudException e)
        {
            Utils.writeConsoleMessage("An error occurred while creating capacity pool: " + e.body().message());
            throw e;
        }
    }

    /**
     * Creates a Dual-Protocol Volume
     * @param anfClient Azure NetApp Files Management Client
     * @param volumeParams Contains resource group, Account name, Pool name, and Volume name to use
     * @param volumeBody The Volume body used in the creation
     * @return The newly created Volume
     */
    public static CompletableFuture<VolumeInner> createVolume(AzureNetAppFilesManagementClientImpl anfClient, String[] volumeParams, VolumeInner volumeBody)
    {
        try
        {
            VolumeInner volume = anfClient.volumes().createOrUpdate(volumeParams[0], volumeParams[1], volumeParams[2], volumeParams[3], volumeBody);
            Utils.writeSuccessMessage("Volume successfully created, resourceId: " + volume.id());

            return CompletableFuture.completedFuture(volume);
        }
        catch (CloudException e)
        {
            Utils.writeConsoleMessage("An error occurred while creating volume: " + e.body().message());
            throw e;
        }
    }
}
