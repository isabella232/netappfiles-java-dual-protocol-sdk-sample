// Copyright (c) Microsoft and contributors.  All rights reserved.
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package dualprotocol.sdk.sample;

import com.microsoft.azure.management.netapp.v2020_06_01.implementation.AzureNetAppFilesManagementClientImpl;
import dualprotocol.sdk.sample.common.Utils;

import java.util.concurrent.CompletableFuture;

public class Cleanup
{
    /**
     * Deletes all created resources
     * @param anfClient Azure NetApp Files Management Client
     * @param params String array containing account name, pool name, etc, needed to delete resource
     * @param clazz Which resource is being deleted
     */
    public static <T> CompletableFuture<Void> runCleanupTask(AzureNetAppFilesManagementClientImpl anfClient, String[] params, Class<T> clazz)
    {
        switch (clazz.getSimpleName())
        {
            case "VolumeInner":
                Utils.writeConsoleMessage("Deleting Volume...");
                anfClient.volumes().delete(
                        params[0],
                        params[1],
                        params[2],
                        params[3]);
                break;

            case "CapacityPoolInner":
                Utils.writeConsoleMessage("Deleting Capacity Pool...");
                anfClient.pools().delete(
                        params[0],
                        params[1],
                        params[2]);
                break;

            case "NetAppAccountInner":
                Utils.writeConsoleMessage("Deleting Account...");
                anfClient.accounts().delete(
                        params[0],
                        params[1]);
                break;

        }

        return CompletableFuture.completedFuture(null);
    }
}
